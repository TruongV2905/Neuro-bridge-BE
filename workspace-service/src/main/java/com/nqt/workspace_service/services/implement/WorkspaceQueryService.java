package com.nqt.workspace_service.services.implement;
import com.nqt.workspace_service.dto.response.PagedResponse;
import com.nqt.workspace_service.dto.response.WorkspaceResponse;
import com.nqt.workspace_service.dto.response.WorkspaceSummaryResponse;
import com.nqt.workspace_service.entities.Workspace;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repositories.WorkspaceRepository;
import com.nqt.workspace_service.services.interfaces.IWorkspaceQueryService;
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

    /**
     * Nghiệp vụ 1: Lấy chi tiết phòng làm việc (Bao gồm kiểm tra bảo mật nội bộ tầng Service)
     */
    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspaceDetails(UUID workspaceId, UUID currentUserId, boolean isAdmin) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy không gian làm việc với ID: " + workspaceId));

        // KIỂM TRA BẢO MẬT: Chỉ Admin hoặc chính Client/Expert trong phòng mới được xem chi tiết
        if (!isAdmin && !workspace.getClient().getUserId().equals(currentUserId)
                && !workspace.getExpert().getUserId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xem thông tin phòng làm việc này!");
        }
        enrichWorkspaceMembers(workspace);
        return convertToResponseDto(workspace);
    }

    /**
     * Nghiệp vụ 2: Phân trang danh sách phòng làm việc theo User ID
     */
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
        boolean isClient = w.getClient().getUserId().equals(currentUserId);
        var partner = isClient ? w.getExpert() : w.getClient();

        // Enrich Partner Info qua gRPC
        try {
            var partnerInfo = userClientService.getUserInfo(partner.getUserId().toString());
            if (partnerInfo != null) {
                partner.setName(partnerInfo.getUsername());
                partner.setAvatarUrl(partnerInfo.getAvatarUrl());
            }
        } catch (Exception e) {
            log.warn("⚠️ Không thể enrich info cho partner {}: {}", partner.getUserId(), e.getMessage());
        }

        return WorkspaceSummaryResponse.builder()
                .workspaceId(w.getWorkspaceId())
                .projectName(w.getProjectDetail().getProjectName())
                .projectStatus(w.getProjectDetail().getStatus().name())
                .progressPercentage(w.getProjectDetail().getProgressPercentage())
                .deadline(w.getProjectDetail().getDeadline())
                .partner(WorkspaceSummaryResponse.PartnerInfo.builder()
                        .name(partner.getName())
                        .avatarUrl(partner.getAvatarUrl())
                        .role(isClient ? "EXPERT" : "CLIENT")
                        .build())
                .build();
    }

    private void enrichWorkspaceMembers(Workspace w) {
        // Tương tự cho Client và Expert
        try {
            var clientInfo = userClientService.getUserInfo(w.getClient().getUserId().toString());
            if (clientInfo != null) {
                w.getClient().setName(clientInfo.getUsername());
                w.getClient().setAvatarUrl(clientInfo.getAvatarUrl());
            }
            var expertInfo = userClientService.getUserInfo(w.getExpert().getUserId().toString());
            if (expertInfo != null) {
                w.getExpert().setName(expertInfo.getUsername());
                w.getExpert().setAvatarUrl(expertInfo.getAvatarUrl());
            }
        } catch (Exception e) {
            log.warn("⚠️ Enrich member thất bại: {}", e.getMessage());
        }
    }

    private WorkspaceResponse convertToResponseDto(Workspace workspace) {
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
