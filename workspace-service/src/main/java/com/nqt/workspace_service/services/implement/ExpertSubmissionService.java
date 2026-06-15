package com.nqt.workspace_service.services.implement;
import com.nqt.workspace_service.dto.request.SubmitPackageRequest;
import com.nqt.workspace_service.dto.response.SubmissionPackageResponse;
import com.nqt.workspace_service.entities.*;
import com.nqt.workspace_service.enums.*;
import com.nqt.workspace_service.grpc.IdentityServiceUnavailableException;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repositories.*;
import com.nqt.workspace_service.services.interfaces.IExpertSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpertSubmissionService implements IExpertSubmissionService {

    private final WorkspaceRepository workspaceRepository;
    private final SubmissionPackageRepository submissionPackageRepository;
    private final UserClientService userClientService;

    @Override
    @Transactional
    public SubmissionPackageResponse submitPackage(SubmitPackageRequest request, UUID expertId) {
        log.info("Expert ID {} đang thực hiện nộp sản phẩm cho Workspace {}", expertId, request.getWorkspaceId());
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy không gian làm việc này!"));

        validateExpertAccess(workspace, expertId);

        int nextVersion = submissionPackageRepository.findMaxVersionByWorkspaceId(request.getWorkspaceId())
                .map(currentMax -> currentMax + 1)
                .orElse(1);
        int finalDays = (request.getCustomReleaseDays() != null) ? request.getCustomReleaseDays() : 3;
        SubmissionPackage submissionPackage = SubmissionPackage.builder()
                .workspace(workspace)
                .version(nextVersion)
                .notes(request.getNotes())
                .status(PackageStatus.PENDING)
                .autoReleaseAt(LocalDateTime.now().plusDays(finalDays)) // Hạn chót tự động duyệt sau 3 ngày
                .build();

        // 5. Ánh xạ danh sách File đính kèm (Composition cháu)
        List<DeliverableFile> deliverableFiles = new ArrayList<>();
        if (request.getFiles() != null) {
            for (var fileReq : request.getFiles()) {
                DeliverableFile file = DeliverableFile.builder()
                        .submissionPackage(submissionPackage) // Khóa ngoại quy đầu về cha nó
                        .fileName(fileReq.getFileName())
                        .fileUrl(fileReq.getFileUrl())
                        .fileSize(fileReq.getFileSize())
                        .fileType(FileType.valueOf(fileReq.getFileType().toUpperCase()))
                        .build();
                deliverableFiles.add(file);
            }
        }
        submissionPackage.setDeliverableFiles(deliverableFiles);

        // 6. Cập nhật trạng thái tiến độ dự án tổng quan sang UNDER_REVIEW
        workspace.getProjectDetail().setStatus(ProjectState.UNDER_REVIEW);
        workspaceRepository.save(workspace);

        // 7. Lưu Package (Cascade lưu luôn cả DeliverableFiles)
        SubmissionPackage savedPackage = submissionPackageRepository.save(submissionPackage);

        return convertToResponseDto(savedPackage);
    }

    private void validateExpertAccess(Workspace workspace, UUID expertId) {
        // Kiểm tra sở hữu trong DB (Căn cứ xác thực chính)
        if (!workspace.getExpert().getUserId().equals(expertId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải Expert được chỉ định cho dự án này!");
        }

        // Kiểm tra danh tính qua gRPC (Cơ chế Fallback)
        try {
            var expertInfo = userClientService.getUserInfo(expertId.toString());
            if (expertInfo == null) {
                throw new IllegalStateException("Expert không tồn tại trên hệ thống!");
            }
            log.info("Expert {} (Name: {}) đang thực hiện nộp sản phẩm.", expertId, expertInfo.getUsername());
        } catch (IdentityServiceUnavailableException e) {
            log.warn("⚠️ IdentityService không khả dụng. Fallback: Cho phép nộp bài dựa trên xác thực DB.");
        } catch (Exception e) {
            log.error("Lỗi xác thực gRPC cho expert {}: {}", expertId, e.getMessage());
            throw new RuntimeException("Không thể xác thực người dùng hiện tại!");
        }
    }

    private SubmissionPackageResponse convertToResponseDto(SubmissionPackage sp) {
        return SubmissionPackageResponse.builder()
                .packageId(sp.getPackageId())
                .workspaceId(sp.getWorkspace().getWorkspaceId())
                .version(sp.getVersion())
                .notes(sp.getNotes())
                .submittedAt(sp.getSubmittedAt())
                .reviewedAt(sp.getReviewedAt())
                .autoReleaseAt(sp.getAutoReleaseAt())
                .status(sp.getStatus())
                .feedback(sp.getFeedback())
                .files(sp.getDeliverableFiles().stream()
                        .map(f -> SubmissionPackageResponse.FileDetails.builder()
                                .fileId(f.getFileId())
                                .fileName(f.getFileName())
                                .fileUrl(f.getFileUrl())
                                .fileSize(f.getFileSize())
                                .fileType(f.getFileType().name())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
