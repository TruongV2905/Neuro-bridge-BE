package com.nqt.rating_service.integration.workspace;

import java.util.UUID;

public interface WorkspaceRatingClient {
    WorkspaceRatingEligibility getRatingEligibility(UUID workspaceId, UUID reviewerId);
}
