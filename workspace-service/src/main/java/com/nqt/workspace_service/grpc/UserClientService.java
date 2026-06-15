package com.nqt.workspace_service.grpc;

import com.yourcompany.grpc.UserRequest;
import com.yourcompany.grpc.UserResponse;
import com.yourcompany.grpc.UserServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserClientService {
    private static final Logger logger = LoggerFactory.getLogger(UserClientService.class);

    @GrpcClient("identity-service")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    public UserResponse getUserInfo(String userId) {
        UserRequest request = UserRequest.newBuilder().setUserId(userId).build();
        try {
            return userStub.getUser(request);
        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();
            if (code == Status.Code.UNAVAILABLE) {
                logger.warn("Identity service unavailable when fetching userId={}", userId, e);
                throw new IdentityServiceUnavailableException("Identity service is unavailable", e);
            } else if (code == Status.Code.NOT_FOUND) {
                logger.info("User not found in identity-service: {}", userId);
                return null;
            } else {
                logger.error("gRPC error when calling identity-service for userId={}: {}", userId, e.getStatus(), e);
                throw e;
            }
        }
    }
}

