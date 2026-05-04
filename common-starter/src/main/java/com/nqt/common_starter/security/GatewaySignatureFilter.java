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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String userId = request.getHeader("X-User-Id");
            String roles = request.getHeader("X-User-Roles");
            String permissions = request.getHeader("X-User-Permissions");
            String signature = request.getHeader("X-Signature");
            log.info("UerId: {}", userId);
            log.info("Roles: {}", roles);
            log.info("Permissions: {}", permissions);
            if (userId == null || signature == null) {

                log.warn("Missing headers for path: {}", request.getServletPath());
                throw new SecurityException("Missing authentication headers");
            }

            // Verify HMAC





            String dataToVerify = userId
                    + (roles != null ? roles : "")
                    + (permissions != null ? permissions : "");

            byte[] decodedKey = Base64.decodeBase64(SHARED_SECRET);
            String expectedSignature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, decodedKey)
                    .hmacHex(dataToVerify);

            if (!expectedSignature.equals(signature)) {
                log.error("Invalid signature for user: {}", userId);
                throw new GlobalException(ErrorCode.UNAUTHENTICATED);
            }

            // Set Authentication
            List<SimpleGrantedAuthority> authorities = Arrays.stream(
                            (permissions != null && !permissions.isEmpty() ? permissions.split(",") : new String[0])
                    ).map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            if (roles != null && !roles.isEmpty()) {
                Arrays.stream(roles.split(","))
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }

            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            // Clear context if error
            SecurityContextHolder.clearContext();
            // Tùy chọn: Trả về 401 trực tiếp tại đây nếu muốn chặt chẽ
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Gateway Signature");
        }
    }
}