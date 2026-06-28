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
        if (workspaceRepository.existsByJobId(request.getJobId())) {
            throw new IllegalStateException("Workspace cho công việc này đã tồn tại!");
        }

        Workspace workspace = Workspace.builder()
                .jobId(request.getJobId())
                .escrowId(request.getEscrowId())
                .clientId(request.getClientId())
                .expertId(request.getExpertId())
                .status(WorkspaceStatus.ACTIVE)
                .build();

        ProjectDetail projectDetail = ProjectDetail.builder()
                .workspace(workspace)
                .projectName(request.getProjectName())
                .startDate(LocalDateTime.now())
                .deadline(request.getDeadline())
                .status(ProjectState.IN_PROGRESS)
                .progressPercentage(0.0f)
                .build();

        workspace.setProjectDetail(projectDetail);
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // Tạo member SAU khi có workspaceId
        WorkspaceMember client = getOrCreateMember(
                request.getClientId(), WorkspaceRole.CLIENT, savedWorkspace.getWorkspaceId());
        WorkspaceMember expert = getOrCreateMember(
                request.getExpertId(), WorkspaceRole.EXPERT, savedWorkspace.getWorkspaceId());

        return convertToResponseDto(savedWorkspace, client, expert);
    }

    private WorkspaceMember getOrCreateMember(UUID userId, WorkspaceRole role, UUID workspaceId) {
        return memberRepository.findByUserId(userId).orElseGet(() -> {
            String name = "User " + userId.toString().substring(0, 5);
            String avatar = null;
            try {
                var userInfo = userClientService.getUserInfo(userId.toString());
                if (userInfo != null) {
                    name = userInfo.getUsername();
                    avatar = userInfo.getAvatarUrl();
                }
            } catch (Exception e) {
                log.warn("⚠️ Không thể lấy thông tin user {} từ IdentityService. Dùng giá trị mặc định.", userId);
            }
            return memberRepository.save(WorkspaceMember.builder()
                    .userId(userId)
                    .name(name)
                    .role(role)
                    .avatarUrl(avatar)
                    .workspaceId(workspaceId) // gán workspaceId ở đây
                    .build());
        });
    }

    /**
     * Hàm phụ trợ: Chuyển đổi dữ liệu từ Entity sang cấu trúc phẳng DTO
     */
    @Override
    public WorkspaceResponse convertToResponseDto(Workspace workspace,
                                                  WorkspaceMember client,
                                                  WorkspaceMember expert) {
        return WorkspaceResponse.builder()
                .workspaceId(workspace.getWorkspaceId())
                .jobId(workspace.getJobId())
                .escrowId(workspace.getEscrowId())
                .status(workspace.getStatus())
                .createdAt(workspace.getCreatedAt())
                .client(WorkspaceResponse.MemberInfo.builder()
                        .userId(client.getUserId())
                        .name(client.getName())
                        .avatarUrl(client.getAvatarUrl())
                        .build())
                .expert(WorkspaceResponse.MemberInfo.builder()
                        .userId(expert.getUserId())
                        .name(expert.getName())
                        .avatarUrl(expert.getAvatarUrl())
                        .build())
                .projectName(workspace.getProjectDetail().getProjectName())
                .deadline(workspace.getProjectDetail().getDeadline())
                .startDate(workspace.getProjectDetail().getStartDate())
                .projectStatus(workspace.getProjectDetail().getStatus().name())
                .progressPercentage(workspace.getProjectDetail().getProgressPercentage())
                .build();
    }
}