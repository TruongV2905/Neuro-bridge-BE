package com.nqt.workspace_service.services.implement;
import com.nqt.workspace_service.dto.response.DeliverableFileRespond;
import com.nqt.workspace_service.dto.response.PackageResRespond;
import com.nqt.workspace_service.dto.response.PackageRespond;
import com.nqt.workspace_service.entities.SubmissionPackage;
import com.nqt.workspace_service.repositories.SubmissionPackageRepository;
import com.nqt.workspace_service.repositories.WorkspaceMemberRepository;
import com.nqt.workspace_service.services.interfaces.ISubmissionPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionPackageService implements ISubmissionPackageService {

    private final SubmissionPackageRepository packageRepository;
    private final WorkspaceMemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PackageRespond> getPackagesByWorkspace(UUID workspaceId, UUID currentUserId) {
        log.info("User {} đang yêu cầu lấy danh sách gói nộp bài của Workspace {}", currentUserId, workspaceId);

        // Bảo mật chéo: Chỉ thành viên trong Workspace mới được xem danh sách gói nộp bài
        memberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Quyền truy cập bị từ chối: Bạn không thuộc Workspace này!"));

        return packageRepository.findByWorkspaceIdAndNotDeleted(workspaceId).stream()
                .map(this::mapToPackageRespond)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PackageResRespond getPackageById(UUID packageId, UUID currentUserId) {
        log.info("User {} đang yêu cầu lấy thông tin chi tiết gói nộp bài {}", currentUserId, packageId);

        SubmissionPackage submissionPackage = packageRepository.findByIdAndNotDeleted(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói nộp bài yêu cầu hoặc đã bị xóa!"));

        // Bảo mật chéo: Kiểm tra quyền xem thông qua Workspace ID của gói nộp bài đó
        UUID workspaceId = submissionPackage.getWorkspace().getWorkspaceId();
        memberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Quyền truy cập bị từ chối: Bạn không có quyền xem thông tin gói nộp bài này!"));

        return mapToPackageResRespond(submissionPackage);
    }

    private PackageRespond mapToPackageRespond(SubmissionPackage sp) {
        return new PackageRespond(
                sp.getPackageId(),
                sp.getWorkspace() != null ? sp.getWorkspace().getWorkspaceId() : null,
                sp.getVersion(),
                sp.getNotes(),
                sp.getSubmittedAt(),
                sp.getReviewedAt(),
                sp.getAutoReleaseAt(),
                sp.getStatus(),
                sp.getFeedback(),
                sp.getIsDelete()
        );
    }

    private PackageResRespond mapToPackageResRespond(SubmissionPackage sp) {
        List<DeliverableFileRespond> files = sp.getDeliverableFiles().stream()
                .filter(file -> !Boolean.TRUE.equals(file.getIsDelete())) // Chỉ map các file chưa xóa
                .map(file -> new DeliverableFileRespond(
                        file.getFileId(),
                        sp.getPackageId(),
                        file.getFileName(),
                        file.getFileUrl(),
                        file.getFileSize(),
                        file.getFileType(),
                        file.getUploadedAt(),
                        file.getIsDelete()
                )).collect(Collectors.toList());

        return new PackageResRespond(
                sp.getPackageId(),
                sp.getWorkspace() != null ? sp.getWorkspace().getWorkspaceId() : null,
                sp.getVersion(),
                sp.getNotes(),
                sp.getSubmittedAt(),
                sp.getReviewedAt(),
                sp.getAutoReleaseAt(),
                sp.getStatus(),
                sp.getFeedback(),
                sp.getIsDelete(),
                files
        );
    }
}
