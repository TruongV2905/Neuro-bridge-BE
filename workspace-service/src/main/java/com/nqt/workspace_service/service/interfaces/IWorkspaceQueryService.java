package com.nqt.workspace_service.service.interfaces;
import com.nqt.workspace_service.dto.response.PagedResponse;
import com.nqt.workspace_service.dto.response.WorkspaceResponse;
import com.nqt.workspace_service.dto.response.WorkspaceSummaryResponse;

import java.util.UUID;

public interface IWorkspaceQueryService {
    WorkspaceResponse getWorkspaceDetails(UUID workspaceId, UUID currentUserId, boolean isAdmin);
    PagedResponse<WorkspaceSummaryResponse> getWorkspacesByUser(UUID userId, int pageNo, int pageSize);
}
