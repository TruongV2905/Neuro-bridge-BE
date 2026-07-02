package com.nqt.identity_service.grpc;

import com.identity.grpc.GetCurrentUserRequest;
import com.identity.grpc.UserResponse;
import com.identity.grpc.UserServiceGrpc;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.identity_service.service.UserService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserGrpcServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    UserService userService;

    @Override
    public void getCurrentUser(
            GetCurrentUserRequest request,
            StreamObserver<UserResponse> responseObserver
    ) {
        try {
            com.nqt.identity_service.dto.response.UserResponse user =
                    userService.getCurrentUser();

            UserResponse response = toGrpcUserResponse(user);

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

    private UserResponse toGrpcUserResponse(
            com.nqt.identity_service.dto.response.UserResponse user
    ) {
        return UserResponse.newBuilder()
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
                                ? List.of()
                                : user.getSkillIds()
                )
                .build();
    }

    private String toStringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}