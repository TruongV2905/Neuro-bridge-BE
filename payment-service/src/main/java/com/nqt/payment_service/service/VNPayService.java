package com.nqt.payment_service.service;

import com.nqt.payment_service.repository.PaymentTransactionRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
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
        String createDate = LocalDateTime.now().format(formatter);

        String orderIdVNPay = UUID.randomUUID().toString().replace("-", "");

        String vnAmount = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString();

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
        vnpParams.put("vnp_ReturnUrl", returnUrl.trim() + "?paymentTransactionId=" + paymentTransactionId);
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_IpAddr", clientIp);

        StringBuilder  signDataBuilder = new StringBuilder();
        for(Map.Entry<String, String> entry : vnpParams.entrySet()){
            signDataBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            signDataBuilder.append("=");
            signDataBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            signDataBuilder.append("&");
        }

        signDataBuilder.deleteCharAt(signDataBuilder.length() - 1);
        String sigData = signDataBuilder.toString();
        String singed = generateHMAC(hashSecret, sigData);

        vnpParams.put("vnp_SecureHash", singed);

        StringBuilder urlBuilder = new StringBuilder(payUrl.trim()).append("?");
        for(Map.Entry<String, String> entry : vnpParams.entrySet()){
            urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            urlBuilder.append("=");
            urlBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            urlBuilder.append("&");
        }

        urlBuilder.deleteCharAt(urlBuilder.length() - 1);

        return urlBuilder.toString();
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
