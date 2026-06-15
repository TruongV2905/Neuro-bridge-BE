package com.nqt.workspace_service.services.implement;
import com.nqt.workspace_service.dto.request.CreateWorkspaceRequest;
import com.nqt.workspace_service.dto.response.WorkspaceResponse;
import com.nqt.workspace_service.entities.ProjectDetail;
import com.nqt.workspace_service.entities.Workspace;
import com.nqt.workspace_service.entities.WorkspaceMember;
import com.nqt.workspace_service.enums.ProjectState;
import com.nqt.workspace_service.enums.WorkspaceRole;
import com.nqt.workspace_service.enums.WorkspaceStatus;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repositories.WorkspaceMemberRepository;
import com.nqt.workspace_service.repositories.WorkspaceRepository;
import com.nqt.workspace_service.services.interfaces.IWorkspaceInitializationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceInitializationService implements IWorkspaceInitializationService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserClientService userClientService;

    @Override
    @Transactional
    public WorkspaceResponse initializeWorkspace(CreateWorkspaceRequest request) {
        log.info("Bắt đầu xử lý khởi tạo Workspace cho Job ID: {}", request.getJobId());

        // 1. Kiểm tra phòng ngừa trùng lặp Workspace cho một công việc
        if (workspaceRepository.existsByJobId(request.getJobId())) {
            throw new IllegalStateException("Workspace cho công việc này đã tồn tại!");
        }

        WorkspaceMember client = getOrCreateMember(request.getClientId(), WorkspaceRole.CLIENT);
        WorkspaceMember expert = getOrCreateMember(request.getExpertId(), WorkspaceRole.EXPERT);

        // 3. Khởi tạo thực thể gốc Workspace
        Workspace workspace = Workspace.builder()
                .jobId(request.getJobId())
                .escrowId(request.getEscrowId())
                .client(client)
                .expert(expert)
                .status(WorkspaceStatus.ACTIVE)
                .build();

        // 4. Khởi tạo cấu hình chi tiết dự án ban đầu (ProjectDetail - 1-1 Composition)
        ProjectDetail projectDetail = ProjectDetail.builder()
                .workspace(workspace) // Gắn ngược tham chiếu về thực thể cha
                .projectName(request.getProjectName())
                .startDate(LocalDateTime.now())
                .deadline(request.getDeadline())
                .status(ProjectState.IN_PROGRESS)
                .progressPercentage(0.0f)
                .build();

        workspace.setProjectDetail(projectDetail);

        // 5. Lưu xuống DB qua thực thể gốc (Cascade tự động chèn ProjectDetail)
        Workspace savedWorkspace = workspaceRepository.save(workspace);
        log.info("Khởi tạo thành công Workspace với ID: {}", savedWorkspace.getWorkspaceId());

        // 6. Chuyển đổi sang Response DTO để trả về tầng Controller
        return convertToResponseDto(savedWorkspace);
    }

    private WorkspaceMember getOrCreateMember(UUID userId, WorkspaceRole role) {
        return memberRepository.findById(userId).orElseGet(() -> {
            String name = "User " + userId.toString().substring(0, 5); // Giá trị fallback
            String avatar = null;

            try {
                var userInfo = userClientService.getUserInfo(userId.toString());
                if (userInfo != null) {
                    name = userInfo.getUsername();
                    avatar = userInfo.getAvatarUrl(); // Nếu bạn thêm trường avatar trong proto
                }
            } catch (Exception e) {
                log.warn("⚠️ Không thể lấy thông tin user {} từ IdentityService ({}). Dùng giá trị mặc định.", userId, e.getMessage());
            }

            return memberRepository.save(WorkspaceMember.builder()
                    .userId(userId)
                    .name(name)
                    .role(role)
                    .avatarUrl(avatar)
                    .build());
        });
    }

    /**
     * Hàm phụ trợ: Chuyển đổi dữ liệu từ Entity sang cấu trúc phẳng DTO
     */
    @Override
    public WorkspaceResponse convertToResponseDto(Workspace workspace) {
        if (workspace == null) return null;

        return WorkspaceResponse.builder()
                .workspaceId(workspace.getWorkspaceId())
                .jobId(workspace.getJobId())
                .escrowId(workspace.getEscrowId())
                .status(workspace.getStatus())
                .createdAt(workspace.getCreatedAt())
                .client(WorkspaceResponse.MemberInfo.builder()
                        .userId(workspace.getClient().getUserId())
                        .name(workspace.getClient().getName())
                        .avatarUrl(workspace.getClient().getAvatarUrl())
                        .build())
                .expert(WorkspaceResponse.MemberInfo.builder()
                        .userId(workspace.getExpert().getUserId())
                        .name(workspace.getExpert().getName())
                        .avatarUrl(workspace.getExpert().getAvatarUrl())
                        .build())
                .projectName(workspace.getProjectDetail().getProjectName())
                .deadline(workspace.getProjectDetail().getDeadline())
                .startDate(workspace.getProjectDetail().getStartDate())
                .projectStatus(workspace.getProjectDetail().getStatus().name())
                .progressPercentage(workspace.getProjectDetail().getProgressPercentage())
                .build();
    }
}