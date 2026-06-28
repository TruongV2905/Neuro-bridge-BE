package com.nqt.common_starter.config;

import com.nqt.common_starter.security.GrpcJwtAuthenticationToken;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

@Slf4j
public class JwtGrpcAuthenticationReader implements GrpcAuthenticationReader {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public Authentication readAuthentication(
            ServerCall<?, ?> call,
            Metadata headers
    ) {
        String authorization = headers.get(AUTHORIZATION);

        log.info("JwtGrpcAuthenticationReader.readAuthentication() called — authorization present: {}",
                authorization != null && !authorization.isBlank());

        if (authorization == null || authorization.isBlank()) {
            throw new BadCredentialsException("Missing gRPC Authorization metadata");
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new BadCredentialsException("Invalid gRPC Authorization metadata");
        }

        String token = authorization.substring(BEARER_PREFIX.length());

        return new GrpcJwtAuthenticationToken(token);
    }
}