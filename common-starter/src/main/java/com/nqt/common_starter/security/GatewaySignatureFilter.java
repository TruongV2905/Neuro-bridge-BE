package com.nqt.common_starter.security;


import com.nqt.common_starter.config.SecurityProperties;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class GatewaySignatureFilter extends OncePerRequestFilter {

    @Value("${app.shared-secret}")
    String SHARED_SECRET;

    final SecurityProperties securityProperties;

    final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return securityProperties.getPublicEndpoints().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String userId = request.getHeader("X-User-Id");
            String role = request.getHeader("X-User-Role");
            String timestamp = request.getHeader("X-Timestamp");
            String requestId = request.getHeader("X-Request-Id");
            String signature = request.getHeader("X-Signature");

            log.info("UserId: {}", userId);
            log.info("Role: {}", role);

            if (userId == null || role == null || signature == null
                    || timestamp == null || requestId == null) {
                throw new GlobalException(ErrorCode.UNAUTHENTICATED.getMessage());
            }

            long ts = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();

            if (Math.abs(now - ts) > 300) {
                throw new GlobalException(ErrorCode.UNAUTHENTICATED.getMessage());
            }

            String dataToVerify = String.join("|",
                    userId,
                    role,
                    timestamp,
                    requestId
            );

            byte[] decodedKey = Base64.decodeBase64(SHARED_SECRET);

            String expectedSignature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, decodedKey)
                    .hmacHex(dataToVerify);

            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8))) {
                throw new GlobalException(ErrorCode.UNAUTHENTICATED.getMessage());
            }

            String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority(roleWithPrefix)
            );

            var authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("SecurityContext set: principal={}, authorities={}",
                    authentication.getPrincipal(),
                    authentication.getAuthorities());
        } catch (Exception e) {
            log.error("Gateway authentication error: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Gateway Signature");
            return;
        }

        // để ngoài try-catch
        filterChain.doFilter(request, response);
    }
}