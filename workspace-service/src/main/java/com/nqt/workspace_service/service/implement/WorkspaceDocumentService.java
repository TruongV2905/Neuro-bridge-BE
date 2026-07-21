package com.nqt.workspace_service.service.implement;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.workspace_service.dto.response.WorkspaceDocumentResponse;
import com.nqt.workspace_service.entity.Workspace;
import com.nqt.workspace_service.repository.WorkspaceRepository;
import com.nqt.workspace_service.service.interfaces.IWorkspaceDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceDocumentService implements IWorkspaceDocumentService {

    private final WorkspaceRepository workspaceRepository;

    @Override
    @Transactional(readOnly = true)
    public WorkspaceDocumentResponse getDocument(UUID workspaceId, UUID currentUserId) {
        Workspace workspace = findWorkspace(workspaceId);
        validateWorkspaceMember(workspace, currentUserId);

        return toResponse(workspace.getStrategyDocument());
    }

    @Override
    @Transactional
    public WorkspaceDocumentResponse updateDocument(UUID workspaceId, UUID currentUserId, String newDocument) {
        Workspace workspace = findWorkspace(workspaceId);
        validateExpert(workspace, currentUserId);

        workspace.setStrategyDocument(newDocument);
        workspaceRepository.save(workspace);

        log.info("Expert {} đã cập nhật document cho workspace {}", currentUserId, workspaceId);
        return toResponse(newDocument);
    }

    private WorkspaceDocumentResponse toResponse(String document) {
        return WorkspaceDocumentResponse.builder()
                .document(document)
                .documentRequired(document == null)
                .build();
    }

    private Workspace findWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Workspace " + ErrorCode.NOT_FOUND.getMessage()
                ));
    }

    private void validateWorkspaceMember(Workspace workspace, UUID currentUserId) {
        boolean isMember = workspace.getClientId().equals(currentUserId)
                || workspace.getExpertId().equals(currentUserId);

        if (!isMember) {
            throw new GlobalException(
                    ErrorCode.UNAUTHORIZED.getCode(),
                    "You are not a member of this workspace"
            );
        }
    }

    private void validateExpert(Workspace workspace, UUID currentUserId) {
        if (!workspace.getExpertId().equals(currentUserId)) {
            throw new GlobalException(
                    ErrorCode.UNAUTHORIZED.getCode(),
                    "Only the expert of this workspace can update the document"
            );
        }
    }
}