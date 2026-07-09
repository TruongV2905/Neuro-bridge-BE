package com.nqt.common_starter.security;

import com.nimbusds.jwt.SignedJWT;
import com.nqt.common_starter.exception.GlobalException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.text.ParseException;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JwtAuthenticationSupport {

    public static Authentication toAuthentication(SignedJWT signedJWT) {
        try {
            var claims = signedJWT.getJWTClaimsSet();

            String userId = claims.getStringClaim("userId");

            if (userId == null || userId.isBlank()) {
                userId = claims.getSubject();
            }

            if (userId == null || userId.isBlank()) {
                throw new GlobalException("Missing userId in JWT");
            }

            String role = claims.getStringClaim("role");

            if (role == null || role.isBlank()) {
                role = claims.getStringClaim("scope");
            }

            List<SimpleGrantedAuthority> authorities = buildAuthorities(role);

            return new UsernamePasswordAuthenticationToken(
                    userId,
                    signedJWT,
                    authorities
            );

        } catch (ParseException e) {
            throw new GlobalException("Cannot parse JWT claims");
        }
    }

    private static List<SimpleGrantedAuthority> buildAuthorities(String role) {
        if (role == null || role.isBlank()) {
            return List.of();
        }

        String roleWithPrefix = role.startsWith("ROLE_")
                ? role
                : "ROLE_" + role;

        return List.of(new SimpleGrantedAuthority(roleWithPrefix));
    }
}