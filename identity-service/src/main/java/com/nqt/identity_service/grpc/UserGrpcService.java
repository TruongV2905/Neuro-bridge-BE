package com.nqt.identity_service.grpc;

import com.identity.grpc.UserRequest;
import com.identity.grpc.UserResponse;
import com.identity.grpc.UserServiceGrpc;
import com.nqt.identity_service.repository.UserRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    @Override
    public void getUser(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            var user = userRepository.findById(UUID.fromString(request.getUserId()))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserResponse response = UserResponse.newBuilder()
                    .setId(user.getId().toString())
                    .setFullName(user.getFullName() != null ? user.getFullName() : "")
                    .setEmail(user.getEmail() != null ? user.getEmail() : "")
                    .setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
}