package com.nqt.chat_service.httpclient;

import com.identity.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class IdentityServiceClient {

    @GrpcClient("identity-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;


    /**
     * Lấy user hiện tại (đã đăng nhập) từ identity-service qua gRPC.
     */
    public UserResponse getCurrentUser() {
        return userServiceBlockingStub.getCurrentUser(
                GetCurrentUserRequest.newBuilder().build()
        );
    }

    /**
     * Lấy user theo code (vd: "CLIENT001", "EXPERT001") qua gRPC.
     */
    public UserResponse getUserByCode(String code) {
        return userServiceBlockingStub.getUserByCode(
                UserCodeRequest.newBuilder().setCode(code).build()
        );
    }

    public UserResponse getUserById(String userId) {
        return userServiceBlockingStub.getUser(
                UserRequest.newBuilder().setUserId(userId).build()
        );
    }

}
