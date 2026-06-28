package com.nqt.jobPost_service.httpclient;

import com.identity.grpc.GetCurrentUserRequest;
import com.identity.grpc.SkillSuggestionRequest;
import com.identity.grpc.SkillSuggestionResponse;
import com.identity.grpc.SkillSuggestionServiceGrpc;
import com.identity.grpc.UserResponse;
import com.identity.grpc.UserServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class IdentityServiceClient {

    @GrpcClient("identity-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    @GrpcClient("identity-service")
    private SkillSuggestionServiceGrpc.SkillSuggestionServiceBlockingStub skillSuggestionBlockingStub;

    public UserResponse getCurrentUser() {
        return userServiceBlockingStub.getCurrentUser(
                GetCurrentUserRequest.newBuilder().build()
        );
    }

    public SkillSuggestionResponse suggestSkills(String title, String description) {
        SkillSuggestionRequest request = SkillSuggestionRequest.newBuilder()
                .setTitle(safeString(title))
                .setDescription(safeString(description))
                .build();

        return skillSuggestionBlockingStub.suggestSkills(request);
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}