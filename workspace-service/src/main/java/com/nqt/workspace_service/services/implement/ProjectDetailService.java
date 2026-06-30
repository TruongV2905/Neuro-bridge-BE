package com.nqt.workspace_service.services.implement;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.workspace_service.dto.request.*;
import com.nqt.workspace_service.dto.response.ProjectDetailRespond;
import com.nqt.workspace_service.entities.ProjectDetail;
import com.nqt.workspace_service.enums.ProjectState;
import com.nqt.workspace_service.grpc.IdentityServiceUnavailableException;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repositories.ProjectDetailRepository;
import com.nqt.workspace_service.services.interfaces.IProjectDetailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j

public class ProjectDetailService implements IProjectDetailService {
    ProjectDetailRepository projectRepository;
    UserClientService userClientService;

    /**
     * 1. + getProjectDetail(workspaceId: UUID): ProjectDetailRespond
     * Quyền: Phải là thành viên trong Workspace mới được xem dự án
     */
    @PreAuthorize("hasRole('ADMIN') or @workspaceSecurityExpression.isMemberOf(#workspaceId)")
    public ProjectDetailRespond getProjectDetail(UUID workspaceId) {
        log.info("Fetching project details for workspaceId: {}", workspaceId);
        validateUserIdentity();
        return projectRepository.findProjectDetailByWorkspaceId(workspaceId)
                .map(this::mapToRespond)
                .orElseThrow(() -> new GlobalException("Project not found for this workspace"));
    }

    /**
     * 2. + updateStatus(req: UpdateStateReqDTO): ProjectDetailRespond
     * Quyền: Thành viên thuộc Workspace của dự án này mới được sửa
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @workspaceSecurityExpression.isProjectOwner(#req.projectId)")
    public ProjectDetailRespond updateStatus(UpdateStateRequest req) {
        log.info("Updating status for projectId: {} to {}", req.getProjectId(), req.getStatus());
        validateUserIdentity();
        ProjectDetail project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new GlobalException("Project not found"));

        project.setStatus(req.getStatus());
        if (req.getStatus() == ProjectState.COMPLETED) {
            project.setCompletedAt(LocalDateTime.now());
            project.setProgressPercentage(100.0f);
        } else {
            project.setCompletedAt(null);
        }

        return mapToRespond(projectRepository.save(project));
    }

    /**
     * 3. + updateProgress(req: UpdateProgressReqDTO): ProjectDetailRespond
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @workspaceSecurityExpression.isProjectOwner(#req.projectId)")
    public ProjectDetailRespond updateProgress(UpdateProgressRequest req) {
        log.info("Updating progress for projectId: {} to {}%", req.getProjectId(), req.getProgressPercentage());
        ProjectDetail project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new GlobalException("Project not found"));

        project.setProgressPercentage(req.getProgressPercentage());
        if (req.getProgressPercentage() >= 100.0f) {
            project.setStatus(ProjectState.COMPLETED);
            project.setCompletedAt(LocalDateTime.now());
        }

        return mapToRespond(projectRepository.save(project));
    }

    /**
     * 4. + updateProjectInfo(req: UpdateProjectReqDTO): ProjectDetailRespond
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @workspaceSecurityExpression.isProjectOwner(#req.projectId)")
    public ProjectDetailRespond updateProjectInfo(UpdateProjectRequest req) {
        log.info("Updating general info for projectId: {}", req.getProjectId());
        ProjectDetail project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new GlobalException("Project not found"));

        project.setProjectName(req.getProjectName());
        project.setStartDate(req.getStartDate());
        project.setDeadline(req.getDeadline());

        return mapToRespond(projectRepository.save(project));
    }

    private void validateUserIdentity() {
        // Lấy userId từ SecurityContext thông qua Spring Security
        String currentUserIdStr = getCurrentUserIdFromContext();
        if (currentUserIdStr == null) {
            throw new GlobalException("Người dùng không được xác thực!");
        }

        try {
            UUID currentUserId = UUID.fromString(currentUserIdStr);
            var userInfo = userClientService.getUserInfo(currentUserId.toString());
            if (userInfo == null) {
                throw new GlobalException("Người dùng không tồn tại trên hệ thống!");
            }
            log.info("User {} ({}) đang thực hiện thao tác trên Project.", currentUserId, userInfo.getFullName());
        } catch (IdentityServiceUnavailableException e) {
            log.warn("⚠️ IdentityService không khả dụng. Fallback: Cho phép thao tác dựa trên bảo mật của SecurityContext.");
        } catch (IllegalArgumentException e) {
            log.error("UUID không hợp lệ: {}", currentUserIdStr);
            throw new GlobalException("UUID người dùng không hợp lệ!");
        } catch (Exception e) {
            log.error("Lỗi xác thực gRPC: {}", e.getMessage());
            throw new GlobalException("Lỗi xác thực hệ thống!");
        }
    }

    /**
     * Helper method: Lấy userId từ SecurityContext
     */
    private String getCurrentUserIdFromContext() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(authentication.getPrincipal().toString())) {
            log.warn("Unauthenticated request attempted to access workspace resource");
            return null;
        }

        // Trả về userId dạng chuỗi (Do bộ lọc GatewaySignatureFilter gán vào trường Principal)
        return (String) authentication.getPrincipal();
    }

    private ProjectDetailRespond mapToRespond(ProjectDetail entity) {
        return ProjectDetailRespond.builder()
                .projectId(entity.getProjectId())
                .workspaceId(entity.getWorkspace().getWorkspaceId())
                .projectName(entity.getProjectName())
                .deadline(entity.getDeadline())
                .startDate(entity.getStartDate())
                .completedAt(entity.getCompletedAt())
                .status(entity.getStatus())
                .progressPercentage(entity.getProgressPercentage())
                .build();
    }
}
