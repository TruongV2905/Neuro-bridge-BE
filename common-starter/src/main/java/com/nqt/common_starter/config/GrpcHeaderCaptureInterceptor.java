package com.nqt.common_starter.config;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

@Slf4j
@GrpcGlobalServerInterceptor
public class GrpcHeaderCaptureInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        String authorization = headers.get(AUTHORIZATION_METADATA_KEY);

        Context context = Context.current();
        if (authorization != null && !authorization.isBlank()) {
            context = context.withValue(GrpcAuthContext.AUTHORIZATION_CONTEXT_KEY, authorization);
            log.info("GrpcHeaderCaptureInterceptor captured Authorization from incoming metadata");
        } else {
            log.info("GrpcHeaderCaptureInterceptor: no Authorization in incoming metadata");
        }

        return Contexts.interceptCall(context, call, headers, next);
    }
}