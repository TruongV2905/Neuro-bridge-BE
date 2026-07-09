package com.nqt.jobPost_service.httpclient;

import com.identity.grpc.*;
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

    public UserResponse getUserByCode(String code) {
        return userServiceBlockingStub.getUserByCode(
                UserCodeRequest.newBuilder().setCode(code).build()
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