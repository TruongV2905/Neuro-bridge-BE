package com.nqt.workspace_service.config;

import com.nqt.workspace_service.entity.ProjectDetail;
import com.nqt.workspace_service.entity.Workspace;
import com.nqt.workspace_service.repository.ProjectDetailRepository;
import com.nqt.workspace_service.repository.WorkspaceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("workspaceSecurityExpression")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WorkspaceSecurityExpression {

    WorkspaceRepository workspaceRepository;
    ProjectDetailRepository projectRepository;

    /**
     * User hiện tại là thành viên của Workspace khi họ là Client hoặc Expert của Workspace đó.
     */
    public boolean isMemberOf(UUID workspaceId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;

        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            log.warn("Workspace not found with ID: {}", workspaceId);
            return false;
        }

        boolean isMember = isMemberOfWorkspace(workspace, currentUserId);
        if (!isMember) {
            log.warn("Security Alert: User [{}] attempted to access Workspace [{}] without permission",
                    currentUserId, workspaceId);
        }
        return isMember;
    }

    /**
     * User có quyền với Project khi họ là thành viên (Client/Expert) của Workspace chứa Project đó.
     */
    public boolean isProjectOwner(UUID projectId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;

        ProjectDetail project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            log.warn("Project not found with ID: {}", projectId);
            return false;
        }

        boolean isOwner = isMemberOfWorkspace(project.getWorkspace(), currentUserId);
        if (!isOwner) {
            log.warn("Security Alert: User [{}] attempted to modify Project [{}] in Workspace [{}] without permission",
                    currentUserId, projectId, project.getWorkspace().getWorkspaceId());
        }
        return isOwner;
    }

    /**
     * User là Client của Project (dùng cho completeProject / giải ngân escrow).
     */
    public boolean isProjectClient(UUID projectId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;

        ProjectDetail project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            log.warn("Project not found with ID: {}", projectId);
            return false;
        }

        boolean isClient = currentUserId.equals(project.getWorkspace().getClientId());
        if (!isClient) {
            log.warn("Security Alert: User [{}] (not client) attempted to complete Project [{}] in Workspace [{}]",
                    currentUserId, projectId, project.getWorkspace().getWorkspaceId());
        }
        return isClient;
    }

    /**
     * Logic chung: thành viên = Client hoặc Expert của Workspace.
     */
    private boolean isMemberOfWorkspace(Workspace workspace, UUID userId) {
        return userId.equals(workspace.getClientId()) || userId.equals(workspace.getExpertId());
    }

    /**
     * Lấy userId (UUID) từ SecurityContext, trả về null nếu chưa xác thực hoặc UUID sai định dạng.
     */
    private UUID getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(authentication.getPrincipal().toString())) {
            log.warn("Unauthenticated request attempted to access secured workspace resource");
            return null;
        }

        try {
            return UUID.fromString((String) authentication.getPrincipal());
        } catch (IllegalArgumentException | ClassCastException e) {
            log.error("Invalid UUID in Security Context: {}", authentication.getPrincipal());
            return null;
        }
    }
}