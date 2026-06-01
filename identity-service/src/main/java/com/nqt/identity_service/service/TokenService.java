package com.nqt.identity_service.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.identity_service.dto.request.IntrospectRequest;
import com.nqt.identity_service.dto.response.IntrospectResponse;
import com.nqt.identity_service.entity.RefreshToken;
import com.nqt.identity_service.entity.User;
import com.nqt.identity_service.repository.RefreshTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenService {

    RefreshTokenRepository refreshTokenRepository;


    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    @NonFinal
    @Value("${jwt.refresh-duration}")
    protected long REFRESH_DURATION;


    public IntrospectResponse introspect(IntrospectRequest request) {
        var token = request.getToken();

        try {
            SignedJWT signedJWT = verifyToken(token);
            var claimsSet = signedJWT.getJWTClaimsSet();

            // ✅ safe null + trim
            String scope = claimsSet.getStringClaim("scope");

            List<String> authorities = (scope != null && !scope.isBlank())
                    ? Arrays.asList(scope.trim().split("\\s+"))
                    : List.of();

            // ✅ lấy role (có ROLE_ prefix)
            String role = authorities.stream()
                    .filter(s -> s.startsWith("ROLE_"))
                    .map(s -> s.substring(5)) // bỏ ROLE_
                    .findFirst()
                    .orElse(null);

            return IntrospectResponse.builder()
                    .valid(true)
                    .userId(claimsSet.getSubject())
                    .role(role)
                    .build();

        } catch (Exception e) {
            return IntrospectResponse.builder()
                    .valid(false)
                    .build();
        }
    }

    public String generateToken(User user) {

        byte[] keyBytes = Base64.getDecoder().decode(SIGNER_KEY);

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issuer("nqt")
                .audience("nqt-app")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", "ROLE_" + user.getRole().name())
                .build();

        JWSObject jwsObject = new JWSObject(header, new Payload(claims.toJSONObject()));

        try {
            jwsObject.sign(new MACSigner(keyBytes));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new GlobalException(e.getMessage());
        }
    }

    private SignedJWT verifyToken(String token)
            throws JOSEException, ParseException {

        byte[] keyBytes = Base64.getDecoder().decode(SIGNER_KEY);

        SignedJWT signedJWT = SignedJWT.parse(token);

        JWSVerifier verifier = new MACVerifier(keyBytes);

        // 1. verify signature
        boolean verified = signedJWT.verify(verifier);

        if (!verified) {
            throw new GlobalException(ErrorCode.UNAUTHENTICATED.getMessage());
        }

        var claims = signedJWT.getJWTClaimsSet();

        Date now = new Date();

        // 2. check expiration (CHUẨN JWT)
        Date exp = claims.getExpirationTime();

        if (exp == null || exp.before(now)) {
            throw new GlobalException(ErrorCode.UNAUTHENTICATED.getMessage());
        }
        return signedJWT;
    }

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID() + UUID.randomUUID().toString());
        refreshToken.setUserId(user.getId().toString());
        refreshToken.setIssuedAt(Instant.now());
        refreshToken.setRevoked(false);
        refreshToken.setTtl(REFRESH_DURATION * 24 * 60 * 60);

        return refreshTokenRepository.save(refreshToken);
    }


    public Optional<RefreshToken> verifyRefreshToken(String token) {
        return refreshTokenRepository.findById(token).filter(rt -> !rt.isRevoked());
    }

    private void revoke(String token) {
        refreshTokenRepository.findById(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    public void revokeRefreshToken(String token) {
        revoke(token);
    }

    public RefreshToken rotateRefreshToken(String oldToken, User user) {
        revoke(oldToken);
        return createRefreshToken(user);
    }

}
