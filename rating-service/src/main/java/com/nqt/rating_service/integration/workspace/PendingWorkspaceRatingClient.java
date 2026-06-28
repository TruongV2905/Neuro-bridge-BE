package com.nqt.rating_service.integration.workspace;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PendingWorkspaceRatingClient implements WorkspaceRatingClient {
    @Override
    public WorkspaceRatingEligibility getRatingEligibility(UUID workspaceId, UUID reviewerId) {
        // TODO: Replace with workspace-service gRPC integration when the contract is ready.
        // The MVP create endpoint temporarily accepts expertId from the request body.
        return WorkspaceRatingEligibility.builder()
                .workspaceId(workspaceId)
                .reviewerId(reviewerId)
                .eligible(false)
                .reason("Workspace rating eligibility integration is not configured yet")
                .build();
    }
}
