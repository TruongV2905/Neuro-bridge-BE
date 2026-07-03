package com.nqt.common_starter.config;

import com.nimbusds.jwt.SignedJWT;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@GrpcGlobalClientInterceptor
public class GrpcHeaderRelayInterceptor implements ClientInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next
    ) {
        log.info("GrpcHeaderRelayInterceptor INTERCEPT CALL: {}",
                method.getFullMethodName());
        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)
        ) {
            @Override
            public void start(
                    Listener<RespT> responseListener,
                    Metadata metadata
            ) {
                log.info("GrpcHeaderRelayInterceptor START: {}",
                        method.getFullMethodName());
                String authorization = resolveAuthorizationHeader();

                if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                    metadata.put(AUTHORIZATION_METADATA_KEY, authorization);
                    log.info("Relayed Authorization header to gRPC metadata");
                } else {
                    log.info("No Authorization available to relay to gRPC metadata");
                }

                super.start(responseListener, metadata);
            }
        };
    }

    private String resolveAuthorizationHeader() {
        String authorizationFromRequest = resolveFromServletRequest();
        if (authorizationFromRequest != null) {
            return authorizationFromRequest;
        }

        String authorizationFromContext = resolveFromGrpcContext();
        if (authorizationFromContext != null) {
            return authorizationFromContext;
        }

        return resolveFromSecurityContext();
    }

    private String resolveFromGrpcContext() {
        String authorization = GrpcAuthContext.AUTHORIZATION_CONTEXT_KEY.get();
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        return normalizeBearer(authorization);
    }

    private String resolveFromServletRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();

        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }

        HttpServletRequest request = servletAttributes.getRequest();

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        return normalizeBearer(authorization);
    }

    private String resolveFromSecurityContext() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return null;
        }

        Object credentials = authentication.getCredentials();

        if (credentials instanceof String token) {
            return normalizeBearer(token);
        }

        if (credentials instanceof SignedJWT signedJWT) {
            return BEARER_PREFIX + signedJWT.serialize();
        }

        return null;
    }

    private String normalizeBearer(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        if (value.startsWith(BEARER_PREFIX)) {
            return value;
        }

        return BEARER_PREFIX + value;
    }
}