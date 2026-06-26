package com.nqt.common_starter.security;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Slf4j
@Component
public class GatewaySignatureVerifier {

    @Value("${app.shared-secret}")
    private String sharedSecret;

    public void verify(
            String userId,
            String role,
            String timestamp,
            String requestId,
            String signature
    ) {
        if (userId == null || role == null || timestamp == null
                || requestId == null || signature == null) {
            throw new RuntimeException("Missing gateway signature headers");
        }

        long ts;

        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid timestamp");
        }

        long now = Instant.now().getEpochSecond();

        if (Math.abs(now - ts) > 300) {
            throw new RuntimeException("Request timestamp is too old or too far in the future");
        }

        String dataToVerify = String.join("|",
                userId,
                role,
                timestamp,
                requestId
        );

        byte[] decodedKey = Base64.decodeBase64(sharedSecret);

        String expectedSignature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, decodedKey)
                .hmacHex(dataToVerify);

        boolean valid = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.trim().getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            log.error("Wrong signature");
            log.error("dataToVerify: {}", dataToVerify);
            log.error("expectedSignature: {}", expectedSignature);
            log.error("receivedSignature: {}", signature);
            throw new RuntimeException("Wrong signature");
        }
    }
}