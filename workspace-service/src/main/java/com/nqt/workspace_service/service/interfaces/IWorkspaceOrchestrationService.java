package com.nqt.workspace_service.service.interfaces;
import com.nqt.workspace_service.dto.request.DisputeWorkspaceRequest;
import java.util.UUID;

public interface IWorkspaceOrchestrationService {
    void triggerAutoRelease();
    void disputeWorkspace(DisputeWorkspaceRequest request, UUID requesterId, boolean isAdmin);
    void archiveWorkspace(UUID workspaceId);
}
