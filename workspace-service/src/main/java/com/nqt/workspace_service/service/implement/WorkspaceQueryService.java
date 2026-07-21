package com.nqt.workspace_service.service.implement;

import com.nqt.workspace_service.dto.response.PagedResponse;
import com.nqt.workspace_service.dto.response.WorkspaceResponse;
import com.nqt.workspace_service.dto.response.WorkspaceSummaryResponse;
import com.nqt.workspace_service.entity.Workspace;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repository.WorkspaceRepository;
import com.nqt.workspace_service.service.interfaces.IWorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceQueryService implements IWorkspaceQueryService {

    private final WorkspaceRepository workspaceRepository;
    private final UserClientService userClientService;

    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspaceDetails(UUID workspaceId, UUID currentUserId, boolean isAdmin) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy không gian làm việc với ID: " + workspaceId));

        if (!isAdmin && !workspace.getClientId().equals(currentUserId)
                && !workspace.getExpertId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xem thông tin phòng làm việc này!");
        }

        return convertToResponseDto(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WorkspaceSummaryResponse> getWorkspacesByUser(UUID userId, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        Page<Workspace> workspacePage = workspaceRepository.findByUserId(userId, pageable);

        List<WorkspaceSummaryResponse> summaryList = workspacePage.getContent().stream()
                .map(w -> mapToSummary(w, userId))
                .collect(Collectors.toList());

        return PagedResponse.<WorkspaceSummaryResponse>builder()
                .content(summaryList)
                .pageNo(workspacePage.getNumber())
                .pageSize(workspacePage.getSize())
                .totalElements(workspacePage.getTotalElements())
                .totalPages(workspacePage.getTotalPages())
                .last(workspacePage.isLast())
                .build();
    }

    private WorkspaceSummaryResponse mapToSummary(Workspace w, UUID currentUserId) {
        boolean isClient = w.getClientId().equals(currentUserId);
        UUID partnerId = isClient ? w.getExpertId() : w.getClientId();

        String partnerName = "Unknown";
        String partnerAvatar = null;
        try {
            var partnerInfo = userClientService.getUserInfo(partnerId.toString());
            if (partnerInfo != null) {
                partnerName = partnerInfo.getFullName();
                partnerAvatar = partnerInfo.getAvatarUrl();
            }
        } catch (Exception e) {
            log.warn("⚠️ Không thể enrich info cho partner {}: {}", partnerId, e.getMessage());
        }

        return WorkspaceSummaryResponse.builder()
                .workspaceId(w.getWorkspaceId())
                .projectName(w.getProjectDetail().getProjectName())
                .projectStatus(w.getProjectDetail().getStatus().name())
                .progressPercentage(w.getProjectDetail().getProgressPercentage())
                .deadline(w.getProjectDetail().getDeadline())
                .partner(WorkspaceSummaryResponse.PartnerInfo.builder()
                        .name(partnerName)
                        .avatarUrl(partnerAvatar)
                        .role(isClient ? "EXPERT" : "CLIENT")
                        .build())
                .build();
    }

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

    private WorkspaceResponse.MemberInfo resolveMemberInfo(UUID userId) {
        String name = "Unknown";
        String avatar = null;
        try {
            var info = userClientService.getUserInfo(userId.toString());
            if (info != null) {
                name = info.getFullName();
                avatar = info.getAvatarUrl();
            }
        } catch (Exception e) {
            log.warn("⚠️ Enrich member {} thất bại: {}", userId, e.getMessage());
        }
        return WorkspaceResponse.MemberInfo.builder()
                .userId(userId)
                .name(name)
                .avatarUrl(avatar)
                .build();
    }
}