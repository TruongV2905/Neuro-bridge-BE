package com.nqt.identity_service.grpc;

import com.identity.grpc.GetCurrentUserRequest;
import com.identity.grpc.UserResponse;
import com.identity.grpc.UserServiceGrpc;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.identity_service.service.UserService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@GrpcService
@Slf4j
public class UserGrpcServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;

    public UserGrpcServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void getCurrentUser(
            GetCurrentUserRequest request,
            StreamObserver<UserResponse> responseObserver
    ) {
        try {
            Authentication authentication = SecurityContextHolder
                    .getContext()
                    .getAuthentication();

            if (authentication == null || authentication.getName() == null) {
                responseObserver.onError(
                        Status.UNAUTHENTICATED
                                .withDescription("Unauthenticated user")
                                .asRuntimeException()
                );
                return;
            }

            log.info("gRPC getCurrentUser called by userId={}", authentication.getName());

            com.nqt.identity_service.dto.response.UserResponse user =
                    userService.getCurrentUser();

            UserResponse response =
                    UserResponse.newBuilder()
                            .setId(toStringValue(user.getId()))
                            .setCode(toStringValue(user.getCode()))
                            .setFullName(toStringValue(user.getFullName()))
                            .setGender(toStringValue(user.getGender()))
                            .setStatus(toStringValue(user.getStatus()))
                            .setAddress(toStringValue(user.getAddress()))
                            .setAvatarUrl(toStringValue(user.getAvatarUrl()))
                            .setDateOfBirth(toStringValue(user.getDateOfBirth()))
                            .setRole(toStringValue(user.getRole()))
                            .setPhone(toStringValue(user.getPhone()))
                            .setEmail(toStringValue(user.getEmail()))
                            .setCreatedAt(toStringValue(user.getCreatedAt()))
                            .setCompanyName(toStringValue(user.getCompanyName()))
                            .setIndustry(toStringValue(user.getIndustry()))
                            .setCompanyDescription(toStringValue(user.getCompanyDescription()))
                            .addAllSkillIds(
                                    user.getSkillIds() == null
                                            ? java.util.List.of()
                                            : user.getSkillIds()
                            )
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (GlobalException e) {
            log.warn("Business error when getting current user via gRPC: {}", e.getMessage());

            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException()
            );

        } catch (Exception e) {
            log.error("Internal error when getting current user via gRPC", e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to get current user")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    private String toStringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}