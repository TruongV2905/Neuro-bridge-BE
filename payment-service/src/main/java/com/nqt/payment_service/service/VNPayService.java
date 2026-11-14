package com.nqt.payment_service.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class VNPayService {

    @Value("${vnpay.tmn-code}")
    @NonFinal
    String vnpayTmn;

    @Value("${vnpay.hash-secret}")
    @NonFinal
    String hashSecret;

    @Value("${vnpay.pay-url}")
    @NonFinal
    String payUrl;

    @Value("${vnpay.return-url}")
    @NonFinal
    String returnUrl;

    public String createVnPayUrl(String paymentTransactionId, BigDecimal amount, String clientIp)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime now = LocalDateTime.now();
        String createDate = now.format(formatter);
        String expireDate = now.plusMinutes(15).format(formatter);

        String orderIdVNPay = UUID.randomUUID().toString().replace("-", "");

        String vnAmount = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString();

        String safeIp = sanitizeIp(clientIp);

        String safeReturnUrl = returnUrl != null ? returnUrl.trim() : "http://localhost:3000/payment";
        String separator = safeReturnUrl.contains("?") ? "&" : "?";
        String finalReturnUrl = safeReturnUrl + separator + "paymentTransactionId=" + paymentTransactionId;

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayTmn);
        vnpParams.put("vnp_Amount", vnAmount);
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderIdVNPay);
        vnpParams.put("vnp_OrderInfo", "Thanh toan cho ma GD: " + paymentTransactionId);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", finalReturnUrl);
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_ExpireDate", expireDate);
        vnpParams.put("vnp_IpAddr", safeIp);

        // Build data to sign
        StringBuilder signDataBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            signDataBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            signDataBuilder.append("=");
            signDataBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            signDataBuilder.append("&");
        }
        signDataBuilder.deleteCharAt(signDataBuilder.length() - 1);

        String sigData = signDataBuilder.toString();
        String signed = generateHMAC(hashSecret, sigData);
        vnpParams.put("vnp_SecureHash", signed);

        // Build final redirect URL
        StringBuilder urlBuilder = new StringBuilder(payUrl.trim()).append("?");
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            urlBuilder.append("=");
            urlBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            urlBuilder.append("&");
        }
        urlBuilder.deleteCharAt(urlBuilder.length() - 1);

        String paymentUrl = urlBuilder.toString();
        log.info("[VNPay] Generated payment URL for txId={}, amount={}, ip={}", paymentTransactionId, amount, safeIp);
        return paymentUrl;
    }

    private String sanitizeIp(String ip) {
        if (ip == null || ip.isBlank()) return "127.0.0.1";
        if (ip.contains(",")) ip = ip.split(",")[0].trim();
        if (ip.contains(":") || "0:0:0:0:0:0:0:1".equals(ip)) return "127.0.0.1";
        return ip;
    }

    private String generateHMAC(String secretKey, String signData)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmacSha512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmacSha512.init(keySpec);
        byte[] hmacBytes = hmacSha512.doFinal(signData.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();
        for (byte b : hmacBytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
