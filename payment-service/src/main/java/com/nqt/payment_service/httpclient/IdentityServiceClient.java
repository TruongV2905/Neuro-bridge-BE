package com.nqt.payment_service.httpclient;

import com.identity.grpc.GetCurrentUserRequest;
import com.identity.grpc.UserResponse;
import com.identity.grpc.UserServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service

public class IdentityServiceClient {
    @GrpcClient("identity-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;
    public UserResponse getCurrentUser() {
        return userServiceBlockingStub.getCurrentUser(
                GetCurrentUserRequest.newBuilder().build()
        );
    }
}
