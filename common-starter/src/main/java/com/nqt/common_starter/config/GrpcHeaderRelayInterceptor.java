package com.nqt.common_starter.config;

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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@GrpcGlobalClientInterceptor
public class GrpcHeaderRelayInterceptor implements ClientInterceptor {

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

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next
    ) {
        ClientCall<ReqT, RespT> clientCall = next.newCall(method, callOptions);

        return new ForwardingClientCall.SimpleForwardingClientCall<>(clientCall) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata metadata) {

                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    putIfPresent(metadata, X_USER_ID, request, "X-User-Id");
                    putIfPresent(metadata, X_USER_ROLE, request, "X-User-Role");
                    putIfPresent(metadata, X_TIMESTAMP, request, "X-Timestamp");
                    putIfPresent(metadata, X_REQUEST_ID, request, "X-Request-Id");
                    putIfPresent(metadata, X_SIGNATURE, request, "X-Signature");

                    log.debug("Relayed gateway signed headers to gRPC metadata.");
                } else {
                    log.warn("No RequestAttributes found. gRPC metadata will not contain gateway headers.");
                }

                super.start(responseListener, metadata);
            }
        };
    }

    private void putIfPresent(
            Metadata metadata,
            Metadata.Key<String> key,
            HttpServletRequest request,
            String headerName
    ) {
        String value = request.getHeader(headerName);

        if (value != null && !value.isBlank()) {
            metadata.put(key, value);
            log.debug("Relaying gRPC metadata {}: {}", key.name(), value);
        }
    }
}