package com.nqt.common_starter.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nqt.common_starter.exception.GlobalException;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtTokenVerifier {

    private static final String BEARER_PREFIX = "Bearer ";

    @NonFinal
    @Value("${jwt.signerKey}")
    String signerKey;

    @NonFinal
    byte[] keyBytes;

    @PostConstruct
    void init() {
        if (signerKey == null || signerKey.isBlank()) {
            throw new IllegalStateException("Missing jwt.signerKey");
        }

        this.keyBytes = Base64.getDecoder().decode(signerKey);
    }

    public SignedJWT verifyBearer(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new GlobalException("Missing or invalid Authorization header");
        }

        String token = authorization.substring(BEARER_PREFIX.length());

        return verify(token);
    }

    public SignedJWT verify(String token) {
        try {
            return verifyToken(token);
        } catch (Exception e) {
            log.error("JWT verify failed: {}", e.getMessage());
            throw new GlobalException("Invalid JWT");
        }
    }

    public String issueServiceToken(String serviceName) {
        try {
            JWSSigner signer = new MACSigner(keyBytes);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(serviceName)
                    .claim("userId", serviceName)
                    .claim("role", "SERVICE")
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(30))) // sống rất ngắn, chỉ đủ 1 lần gọi nội bộ
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512), claims);

            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Failed to issue service token: {}", e.getMessage());
            throw new GlobalException("Cannot issue service token");
        }
    }

    private SignedJWT verifyToken(String token)
            throws JOSEException, ParseException {

        if (token == null || token.isBlank()) {
            throw new GlobalException("Token is empty");
        }

        SignedJWT signedJWT = SignedJWT.parse(token);

        if (!JWSAlgorithm.HS512.equals(signedJWT.getHeader().getAlgorithm())) {
            throw new GlobalException("Unsupported JWT algorithm");
        }

        JWSVerifier verifier = new MACVerifier(keyBytes);

        boolean verified = signedJWT.verify(verifier);

        if (!verified) {
            throw new GlobalException("Invalid JWT signature");
        }

        var claims = signedJWT.getJWTClaimsSet();

        Date now = new Date();
        Date exp = claims.getExpirationTime();

        if (exp == null || exp.before(now)) {
            throw new GlobalException("JWT expired");
        }

        return signedJWT;
    }
}