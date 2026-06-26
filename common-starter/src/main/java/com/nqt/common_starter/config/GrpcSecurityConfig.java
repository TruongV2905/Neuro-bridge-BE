package com.nqt.common_starter.config;

import com.nqt.common_starter.security.GatewaySignatureVerifier;
import io.grpc.Metadata;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(GrpcAuthenticationReader.class)
public class GrpcSecurityConfig {

    private final GatewaySignatureVerifier gatewaySignatureVerifier;

    private static final Metadata.Key<String> X_USER_ID =
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> X_USER_ROLE =
            Metadata.Key.of("x-user-role", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> X_TIMESTAMP =
            Metadata.Key.of("x-timestamp", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> X_REQUEST_ID =
            Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> X_SIGNATURE =
            Metadata.Key.of("x-signature", Metadata.ASCII_STRING_MARSHALLER);

    @Bean
    @ConditionalOnMissingBean
    public GrpcAuthenticationReader grpcAuthenticationReader() {
        return (call, headers) -> {
            String userId = headers.get(X_USER_ID);
            String role = headers.get(X_USER_ROLE);
            String timestamp = headers.get(X_TIMESTAMP);
            String requestId = headers.get(X_REQUEST_ID);
            String signature = headers.get(X_SIGNATURE);

            if (userId == null || role == null || timestamp == null
                    || requestId == null || signature == null) {
                throw new BadCredentialsException("Missing gRPC signature headers");
            }

            gatewaySignatureVerifier.verify(
                    userId,
                    role,
                    timestamp,
                    requestId,
                    signature
            );

            String roleWithPrefix = role.startsWith("ROLE_")
                    ? role
                    : "ROLE_" + role;

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority(roleWithPrefix))
            );

            return authentication;
        };
    }
}