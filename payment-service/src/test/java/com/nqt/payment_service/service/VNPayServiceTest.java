package com.nqt.payment_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VNPayServiceTest {

    private VNPayService vnPayService;

    @BeforeEach
    void setUp() {
        vnPayService = new VNPayService();
        ReflectionTestUtils.setField(vnPayService, "vnpayTmn", "FCSU007M");
        ReflectionTestUtils.setField(vnPayService, "hashSecret", "YW0OYS5HINCFVVCQM3P75IUOQQO5GO77");
        ReflectionTestUtils.setField(vnPayService, "payUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(vnPayService, "returnUrl", "http://localhost:3000/payment");
    }

    @Test
    @DisplayName("Should generate valid VNPay payment URL and sanitize IPv6 address")
    void testCreateVnPayUrl_Success() throws Exception {
        String txId = UUID.randomUUID().toString();
        BigDecimal amount = new BigDecimal("50000"); // 50,000 VND -> 5,000,000 in VNPay format
        String clientIp = "0:0:0:0:0:0:0:1"; // IPv6 localhost

        String paymentUrl = vnPayService.createVnPayUrl(txId, amount, clientIp);
        System.out.println("TEST_PAYMENT_URL=" + paymentUrl);

        assertNotNull(paymentUrl);
        assertTrue(paymentUrl.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));

        URI uri = new URI(paymentUrl);
        String query = uri.getQuery();
        Map<String, String> queryParams = new HashMap<>();
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            queryParams.put(key, value);
        }

        assertEquals("2.1.0", queryParams.get("vnp_Version"));
        assertEquals("pay", queryParams.get("vnp_Command"));
        assertEquals("FCSU007M", queryParams.get("vnp_TmnCode"));
        assertEquals("5000000", queryParams.get("vnp_Amount"));
        assertEquals("VND", queryParams.get("vnp_CurrCode"));
        assertEquals("127.0.0.1", queryParams.get("vnp_IpAddr"), "IPv6 should be sanitized to 127.0.0.1");
        assertNotNull(queryParams.get("vnp_CreateDate"));
        assertNotNull(queryParams.get("vnp_ExpireDate"));
        assertNotNull(queryParams.get("vnp_TxnRef"));
        assertNotNull(queryParams.get("vnp_SecureHash"));
        assertTrue(queryParams.get("vnp_ReturnUrl").contains("paymentTransactionId=" + txId));
    }
}
