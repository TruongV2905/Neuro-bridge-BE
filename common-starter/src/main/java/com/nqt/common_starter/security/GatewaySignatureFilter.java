package com.nqt.common_starter.security;

import com.nqt.common_starter.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class GatewaySignatureFilter extends OncePerRequestFilter {

    SecurityProperties securityProperties;
    GatewaySignatureVerifier gatewaySignatureVerifier;

    AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return securityProperties.getPublicEndpoints().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String userId = request.getHeader("X-User-Id");
            String role = request.getHeader("X-User-Role");
            String timestamp = request.getHeader("X-Timestamp");
            String requestId = request.getHeader("X-Request-Id");
            String signature = request.getHeader("X-Signature");

            gatewaySignatureVerifier.verify(
                    userId,
                    role,
                    timestamp,
                    requestId,
                    signature
            );

            setAuthentication(userId, role);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Gateway authentication error: {}", e.getMessage());

            SecurityContextHolder.clearContext();
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid Gateway Signature"
            );
        }
    }

    private void setAuthentication(String userId, String role) {
        String roleWithPrefix = role.startsWith("ROLE_")
                ? role
                : "ROLE_" + role;

        var authorities = List.of(
                new SimpleGrantedAuthority(roleWithPrefix)
        );

        var authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                authorities
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}