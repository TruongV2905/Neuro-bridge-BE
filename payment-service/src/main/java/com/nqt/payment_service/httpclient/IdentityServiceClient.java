package com.nqt.payment_service.httpclient;

import com.identity.grpc.GetCurrentUserRequest;
import com.identity.grpc.UserRequest;
import com.identity.grpc.UserResponse;
import com.identity.grpc.UserServiceGrpc;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IdentityServiceClient {

    @GrpcClient("identity-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    public UserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new GlobalException(ErrorCode.UNAUTHENTICATED.getCode(), "User is not authenticated");
        }

        String userId = auth.getName();
        log.info("[Payment] Resolving current user details via gRPC for userId={}", userId);

        try {
            // Priority 1: Call getUser with explicit userId from authenticated JWT
            return userServiceBlockingStub.getUser(
                    UserRequest.newBuilder().setUserId(userId).build()
            );
        } catch (Exception ex) {
            log.warn("[Payment] Direct getUser by id failed for userId={}, trying with token forwarding: {}", userId, ex.getMessage());

            // Priority 2: Forward Bearer token via gRPC metadata if principal is Jwt
            try {
                UserServiceGrpc.UserServiceBlockingStub stub = userServiceBlockingStub;
                if (auth.getPrincipal() instanceof Jwt jwt) {
                    Metadata metadata = new Metadata();
                    metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + jwt.getTokenValue());
                    stub = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
                }
                return stub.getCurrentUser(GetCurrentUserRequest.newBuilder().build());
            } catch (Exception ex2) {
                log.error("[Payment] Failed to get user from identity-service for userId={}: {}. Falling back to userId as code.", userId, ex2.getMessage());
                return UserResponse.newBuilder()
                        .setId(userId)
                        .setCode(userId)
                        .build();
            }
        }
    }

    public UserResponse getUserById(String userId) {
        return userServiceBlockingStub.getUser(
                UserRequest.newBuilder().setUserId(userId).build()
        );
    }
}
