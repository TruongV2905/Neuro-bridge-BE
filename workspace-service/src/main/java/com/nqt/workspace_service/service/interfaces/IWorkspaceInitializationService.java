package com.nqt.workspace_service.service.interfaces;
import com.nqt.workspace_service.dto.request.CreateWorkspaceRequest;
import com.nqt.workspace_service.dto.response.WorkspaceResponse;

public interface IWorkspaceInitializationService {
    WorkspaceResponse initializeWorkspace(CreateWorkspaceRequest request);
}