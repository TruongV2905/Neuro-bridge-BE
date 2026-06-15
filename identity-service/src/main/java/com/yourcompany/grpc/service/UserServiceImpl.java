package com.yourcompany.grpc.service;

import com.yourcompany.grpc.UserRequest;
import com.yourcompany.grpc.UserResponse;
import com.yourcompany.grpc.UserServiceGrpc;
import com.nqt.identity_service.entity.User;
import com.nqt.identity_service.repository.UserRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    // Constructor injection for UserRepository
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void getUser(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        String userId = request.getUserId();
        logger.info("Received GetUser request for userId={}", userId);

        if (userId == null || userId.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("userId is required").asRuntimeException());
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (IllegalArgumentException iae) {
            logger.warn("Invalid UUID provided for userId={}", userId);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid userId").asRuntimeException());
            return;
        }

        try {
            Optional<User> opt = userRepository.findById(uuid);
            if (!opt.isPresent()) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("User not found").asRuntimeException());
                return;
            }

            User user = opt.get();

            UserResponse response = UserResponse.newBuilder()
                    .setUserId(user.getId() != null ? user.getId().toString() : "")
                    .setUsername(user.getUsername() != null ? user.getUsername() : "")
                    .setEmail(user.getEmail() != null ? user.getEmail() : "")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in getUser for userId={}", userId, e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get user: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}

