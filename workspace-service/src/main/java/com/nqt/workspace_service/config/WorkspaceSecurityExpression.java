package com.nqt.workspace_service.config; // 🌟 Sửa lại tên package cho đúng với dự án của bạn

import com.nqt.workspace_service.entities.ProjectDetail;
import com.nqt.workspace_service.repositories.ProjectDetailRepository;
import com.nqt.workspace_service.repositories.WorkspaceMemberRepository;
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

    WorkspaceMemberRepository memberRepository;
    ProjectDetailRepository projectRepository; // Inject thêm repo project để check quyền sửa dự án

    /**
     * Kiểm tra xem User hiện tại đang đăng nhập có phải là THÀNH VIÊN hợp lệ của Workspace hay không.
     * Sử dụng cho API lấy danh sách thành viên hoặc xem thông tin chung của Workspace.
     */
    public boolean isMemberOf(UUID workspaceId) {
        String currentUserIdStr = getCurrentUserIdFromContext();

        if (currentUserIdStr == null) {
            return false;
        }

        try {
            UUID currentUserId = UUID.fromString(currentUserIdStr);

            // Đối soát trong DB xem cặp (workspaceId, userId) này có tồn tại không
            boolean isMember = memberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId).isPresent();
            if (!isMember) {
                log.warn("Security Alert: User [{}] attempted to access Workspace [{}] without permission", currentUserId, workspaceId);
            }
            return isMember;
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format in Security Context: {}", currentUserIdStr);
            return false;
        }
    }

    /**
     * Kiểm tra xem User hiện tại đang đăng nhập có quyền hạn đối với DỰ ÁN (Project) này không.
     * Thỏa mãn khi User thuộc về Workspace đang chứa Dự án đó.
     * Sử dụng cho các API updateStatus, updateProgress, updateProjectInfo.
     */
    public boolean isProjectOwner(UUID projectId) {
        String currentUserIdStr = getCurrentUserIdFromContext();

        if (currentUserIdStr == null) {
            return false;
        }

        try {
            UUID currentUserId = UUID.fromString(currentUserIdStr);

            // 1. Tìm thông tin dự án để xác định nó thuộc Workspace nào
            ProjectDetail project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                log.warn("Project not found with ID: {}", projectId);
                return false;
            }

            // 2. Kiểm tra xem người dùng hiện tại có phải là thành viên của Workspace đó không
            boolean isOwner = memberRepository.findByWorkspaceIdAndUserId(project.getWorkspace().getWorkspaceId(), currentUserId).isPresent();
            if (!isOwner) {
                log.warn("Security Alert: User [{}] attempted to modify Project [{}] in Workspace [{}] without permission",
                        currentUserId, projectId, project.getWorkspace().getWorkspaceId());
            }
            return isOwner;
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format during project owner validation: {}", currentUserIdStr);
            return false;
        }
    }

    /**
     * Hàm Helper bóc tách nhanh chuỗi ID của người dùng đang đăng nhập từ SecurityContext
     */
    private String getCurrentUserIdFromContext() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(authentication.getPrincipal().toString())) {
            log.warn("Unauthenticated request attempted to access secured workspace resource");
            return null;
        }

        // Trả về userId dạng chuỗi (Do bộ lọc GatewaySignatureFilter gán vào trường Principal)
        return (String) authentication.getPrincipal();
    }
}