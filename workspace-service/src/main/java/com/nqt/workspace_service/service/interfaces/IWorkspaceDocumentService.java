package com.nqt.workspace_service.service.interfaces;

import com.nqt.workspace_service.dto.response.WorkspaceDocumentResponse;

import java.util.UUID;

public interface IWorkspaceDocumentService {

    WorkspaceDocumentResponse getDocument(UUID workspaceId, UUID currentUserId);

    WorkspaceDocumentResponse updateDocument(UUID workspaceId, UUID currentUserId, String newDocument);
}
