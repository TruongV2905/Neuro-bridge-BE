package com.nqt.workspace_service.services.implement;
import com.nqt.workspace_service.dto.response.PagedResponse;
import com.nqt.workspace_service.dto.response.WorkspaceResponse;
import com.nqt.workspace_service.dto.response.WorkspaceSummaryResponse;
import com.nqt.workspace_service.entities.Workspace;
import com.nqt.workspace_service.repositories.WorkspaceRepository;
import com.nqt.workspace_service.services.interfaces.IWorkspaceQueryService;
import lombok.RequiredArgsConstructor;
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
public class WorkspaceQueryService implements IWorkspaceQueryService {

    private final WorkspaceRepository workspaceRepository;

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

        return convertToResponseDto(workspace);
    }

    /**
     * Nghiệp vụ 2: Phân trang danh sách phòng làm việc theo User ID
     */
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WorkspaceSummaryResponse> getWorkspacesByUser(UUID userId, int pageNo, int pageSize) {
        // Sắp xếp các phòng làm việc mới khởi tạo lên đầu danh sách (Mới nhất xếp trước)
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());

        Page<Workspace> workspacePage = workspaceRepository.findByUserId(userId, pageable);

        // Chuyển đổi danh sách entities sang danh sách tóm tắt DTO phẳng
        List<WorkspaceSummaryResponse> summaryList = workspacePage.getContent().stream()
                .map(w -> {
                    // Xác định ai là đối tác (Partner) của User đang gọi API để trả về đúng giao diện hiển thị
                    WorkspaceSummaryResponse.PartnerInfo partnerInfo;
                    if (w.getClient().getUserId().equals(userId)) {
                        partnerInfo = WorkspaceSummaryResponse.PartnerInfo.builder()
                                .name(w.getExpert().getName())
                                .avatarUrl(w.getExpert().getAvatarUrl())
                                .role("EXPERT")
                                .build();
                    } else {
                        partnerInfo = WorkspaceSummaryResponse.PartnerInfo.builder()
                                .name(w.getClient().getName())
                                .avatarUrl(w.getClient().getAvatarUrl())
                                .role("CLIENT")
                                .build();
                    }

                    return WorkspaceSummaryResponse.builder()
                            .workspaceId(w.getWorkspaceId())
                            .projectName(w.getProjectDetail().getProjectName())
                            .projectStatus(w.getProjectDetail().getStatus().name())
                            .progressPercentage(w.getProjectDetail().getProgressPercentage())
                            .deadline(w.getProjectDetail().getDeadline())
                            .partner(partnerInfo)
                            .build();
                })
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
