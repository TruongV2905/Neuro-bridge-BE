package com.nqt.common_starter.security;

import com.nimbusds.jwt.SignedJWT;
import com.nqt.common_starter.exception.GlobalException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GrpcJwtAuthenticationProvider implements AuthenticationProvider {

    JwtTokenVerifier jwtTokenVerifier;

    @Override
    public Authentication authenticate(Authentication authentication) {
        log.debug("GrpcJwtAuthenticationProvider.authenticate() called with: {}", authentication);

        try {
            String token = authentication.getCredentials().toString();
            log.debug("Verifying gRPC JWT token (first 30 chars): {}...",
                    token.length() > 30 ? token.substring(0, 30) : token);

            SignedJWT signedJWT = jwtTokenVerifier.verify(token);

            var claims = signedJWT.getJWTClaimsSet();

            String userId = claims.getStringClaim("userId");

            if (userId == null || userId.isBlank()) {
                userId = claims.getSubject();
            }

            if (userId == null || userId.isBlank()) {
                throw new BadCredentialsException("Missing userId in JWT");
            }

            String role = claims.getStringClaim("role");

            if (role == null || role.isBlank()) {
                role = claims.getStringClaim("scope");
            }

            return new GrpcJwtAuthenticationToken(
                    userId,
                    token,
                    buildAuthorities(role)
            );

        } catch (ParseException e) {
            throw new BadCredentialsException("Cannot parse JWT claims", e);
        } catch (GlobalException e) {
            throw new BadCredentialsException(e.getMessage(), e);
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid JWT", e);
        }
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return GrpcJwtAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private List<SimpleGrantedAuthority> buildAuthorities(String role) {
        if (role == null || role.isBlank()) {
            return List.of();
        }

        String roleWithPrefix = role.startsWith("ROLE_")
                ? role
                : "ROLE_" + role;

        return List.of(new SimpleGrantedAuthority(roleWithPrefix));
    }
}