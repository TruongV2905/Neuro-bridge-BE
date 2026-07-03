package com.nqt.common_starter.config;

import io.grpc.Context;

public class GrpcAuthContext {
    public static final Context.Key<String> AUTHORIZATION_CONTEXT_KEY = Context.key("authorization");
}