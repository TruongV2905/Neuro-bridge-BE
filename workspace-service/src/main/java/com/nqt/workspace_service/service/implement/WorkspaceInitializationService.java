package com.nqt.workspace_service.service.implement;

import com.nqt.common_starter.dto.event.NotificationEvent;
import com.nqt.common_starter.kafka.NotificationEventPublisher;
import com.nqt.workspace_service.dto.request.CreateWorkspaceRequest;
import com.nqt.workspace_service.dto.response.WorkspaceResponse;
import com.nqt.workspace_service.entity.ProjectDetail;
import com.nqt.workspace_service.entity.Workspace;
import com.nqt.workspace_service.constant.ProjectState;
import com.nqt.workspace_service.constant.WorkspaceStatus;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repository.WorkspaceRepository;
import com.nqt.workspace_service.service.interfaces.IWorkspaceInitializationService;
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
    private final UserClientService userClientService;
    private final NotificationEventPublisher eventPublisher;

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

        eventPublisher.publish(
                NotificationEventPublisher.WORKSPACE_INVITED,
                NotificationEvent.builder()
                        .eventType("WORKSPACE_INVITED")
                        .recipientUserId(request.getClientId())
                        .senderName("Neural Bridge")
                        .resourceId(savedWorkspace.getWorkspaceId())
                        .workspaceId(savedWorkspace.getWorkspaceId())
                        .build()
        );

        eventPublisher.publish(
                NotificationEventPublisher.WORKSPACE_INVITED,
                NotificationEvent.builder()
                        .eventType("WORKSPACE_INVITED")
                        .recipientUserId(request.getExpertId())
                        .senderName("Neural Bridge")
                        .resourceId(savedWorkspace.getWorkspaceId())
                        .workspaceId(savedWorkspace.getWorkspaceId())
                        .build()
        );

        return convertToResponseDto(savedWorkspace);
    }

    /**
     * Lấy thông tin hiển thị (name, avatar) của 1 user từ IdentityService qua gRPC.
     * Không còn persist vào WorkspaceMember nữa — gọi trực tiếp lúc cần build response.
     */
    private WorkspaceResponse.MemberInfo resolveMemberInfo(UUID userId) {
        String name = "User " + userId.toString().substring(0, 5);
        String avatar = null;
        try {
            var userInfo = userClientService.getUserInfo(userId.toString());
            if (userInfo != null) {
                name = userInfo.getFullName();
                avatar = userInfo.getAvatarUrl();
            }
        } catch (Exception e) {
            log.warn("⚠️ Không thể lấy thông tin user {} từ IdentityService. Dùng giá trị mặc định.", userId);
        }
        return WorkspaceResponse.MemberInfo.builder()
                .userId(userId)
                .name(name)
                .avatarUrl(avatar)
                .build();
    }

    /**
     * Hàm phụ trợ: Chuyển đổi dữ liệu từ Entity sang cấu trúc phẳng DTO.
     * Không nhận WorkspaceMember nữa — dùng clientId/expertId có sẵn trên Workspace
     * rồi resolve name/avatar on-the-fly qua gRPC.
     */
    private WorkspaceResponse convertToResponseDto(Workspace workspace) {
        WorkspaceResponse.MemberInfo client = resolveMemberInfo(workspace.getClientId());
        WorkspaceResponse.MemberInfo expert = resolveMemberInfo(workspace.getExpertId());

        return WorkspaceResponse.builder()
                .workspaceId(workspace.getWorkspaceId())
                .jobId(workspace.getJobId())
                .escrowId(workspace.getEscrowId())
                .status(workspace.getStatus())
                .createdAt(workspace.getCreatedAt())
                .client(client)
                .expert(expert)
                .projectName(workspace.getProjectDetail().getProjectName())
                .deadline(workspace.getProjectDetail().getDeadline())
                .startDate(workspace.getProjectDetail().getStartDate())
                .projectStatus(workspace.getProjectDetail().getStatus().name())
                .progressPercentage(workspace.getProjectDetail().getProgressPercentage())
                .build();
    }
}