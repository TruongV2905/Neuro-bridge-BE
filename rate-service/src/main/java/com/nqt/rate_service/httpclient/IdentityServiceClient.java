package com.nqt.rate_service.httpclient;

import com.identity.grpc.UserCodeRequest;
import com.identity.grpc.UserResponse;
import com.identity.grpc.UserServiceGrpc;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class IdentityServiceClient {

    @GrpcClient("identity-service")
    private UserServiceGrpc.UserServiceBlockingStub identityStub;

    public UserResponse getUserByCode(String code) {
        try {
            return identityStub.getUserByCode(
                    UserCodeRequest.newBuilder()
                            .setCode(code)
                            .build()
            );
        } catch (StatusRuntimeException e) {
            throw e;
        }
    }
}