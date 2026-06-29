package com.nqt.workspace_service.services.implement;
import com.nqt.common_starter.dto.event.NotificationEvent;
import com.nqt.common_starter.kafka.NotificationEventPublisher;
import com.nqt.workspace_service.dto.request.DeliverableFileRequest;
import com.nqt.workspace_service.dto.request.ResubmitPackageRequest;
import com.nqt.workspace_service.dto.request.ReviewPackageRequest;
import com.nqt.workspace_service.dto.request.SubmitPackageRequest;
import com.nqt.workspace_service.dto.response.DeliverableFileRespond;
import com.nqt.workspace_service.dto.response.PackageResRespond;
import com.nqt.workspace_service.dto.response.PackageRespond;
import com.nqt.workspace_service.entities.DeliverableFile;
import com.nqt.workspace_service.entities.SubmissionPackage;
import com.nqt.workspace_service.entities.Workspace;
import com.nqt.workspace_service.entities.WorkspaceMember;
import com.nqt.workspace_service.enums.FileStatus;
import com.nqt.workspace_service.enums.FileType;
import com.nqt.workspace_service.enums.PackageStatus;
import com.nqt.workspace_service.enums.WorkspaceRole;
import com.nqt.workspace_service.grpc.IdentityServiceUnavailableException;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repositories.SubmissionPackageRepository;
import com.nqt.workspace_service.repositories.WorkspaceMemberRepository;
import com.nqt.workspace_service.services.interfaces.ISubmissionPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionPackageService implements ISubmissionPackageService {

    private final SubmissionPackageRepository packageRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserClientService userClientService;
//    private final PackageEventPublisher eventPublisher;
    private final NotificationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<PackageRespond> getPackagesByWorkspace(UUID workspaceId, UUID currentUserId) {
        log.info("User {} đang yêu cầu lấy danh sách gói nộp bài của Workspace {}", currentUserId, workspaceId);

        validateWorkspaceAccess(workspaceId, currentUserId);

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
        validateWorkspaceAccess(workspaceId, currentUserId);

        return mapToPackageResRespond(submissionPackage);
    }

    @Override
    @Transactional
    public PackageRespond submitPackage(SubmitPackageRequest request, UUID currentUserId) {

        // Validate: chỉ EXPERT mới nộp
        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(request.getWorkspaceId(), currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không thuộc Workspace này!"));

        if (!WorkspaceRole.EXPERT.equals(member.getRole())) {
            throw new AccessDeniedException("Chỉ EXPERT mới được nộp bài!");
        }

        // Validate: không có package PENDING đang chờ
        boolean hasPending = packageRepository
                .findByWorkspaceIdAndNotDeleted(request.getWorkspaceId())
                .stream()
                .anyMatch(sp -> PackageStatus.PENDING.equals(sp.getStatus()));
        if (hasPending) {
            throw new IllegalStateException("Đang có gói nộp bài chờ review, không thể nộp thêm!");
        }

        // Validate: đủ 4 file, mỗi FileType xuất hiện đúng 1 lần
        validateFiles(request.getFiles());

        int nextVersion = packageRepository
                .findMaxVersionByWorkspaceId(request.getWorkspaceId())
                .orElse(0) + 1;

        int releaseDays = request.getCustomReleaseDays() != null ? request.getCustomReleaseDays() : 7;

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(request.getWorkspaceId());

        SubmissionPackage pkg = SubmissionPackage.builder()
                .workspace(workspace)
                .version(nextVersion)
                .notes(request.getNotes())
                .autoReleaseAt(LocalDateTime.now().plusDays(releaseDays))
                .status(PackageStatus.PENDING)
                .isDelete(false)
                .build();

        List<DeliverableFile> files = request.getFiles().stream()
                .map(f -> DeliverableFile.builder()
                        .submissionPackage(pkg)
                        .fileName(f.fileName())
                        .fileUrl(f.fileUrl())
                        .fileSize(f.fileSize())
                        .fileType(f.fileType())
                        .fileStatus(FileStatus.PENDING)
                        .isDelete(false)
                        .build())
                .collect(Collectors.toList());

        pkg.getDeliverableFiles().addAll(files);
        SubmissionPackage saved = packageRepository.save(pkg);

        UUID reviewerId = memberRepository
                .findByWorkspaceIdAndRole(request.getWorkspaceId(), WorkspaceRole.CLIENT)
                .map(WorkspaceMember::getUserId)
                .orElse(null); // null thì notification-service không gửi được

        if (reviewerId != null) {
            eventPublisher.publish(
                    NotificationEventPublisher.PACKAGE_SUBMITTED,
                    NotificationEvent.builder()
                            .eventType("PACKAGE_SUBMITTED")
                            .recipientUserId(reviewerId)
                            .senderName(member.getName())
                            .resourceId(saved.getPackageId())
                            .workspaceId(request.getWorkspaceId())
                            .resourceVersion(String.valueOf(saved.getVersion()))
                            .build()
            );
        }
        // ─────────────────────────────────────────────────────────────────────
        return mapToPackageRespond(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public PackageRespond approvePackage(ReviewPackageRequest request, UUID currentUserId) {
        log.info("User {} đang APPROVE gói nộp bài {}", currentUserId, request.getPackageId());

        SubmissionPackage pkg = packageRepository.findByIdAndNotDeleted(request.getPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói nộp bài!"));

        if (!PackageStatus.PENDING.equals(pkg.getStatus())) {
            throw new IllegalStateException("Chỉ có thể review gói đang ở trạng thái PENDING!");
        }

        // Chỉ CLIENT mới được approve
        memberRepository.findByWorkspaceIdAndUserId(pkg.getWorkspace().getWorkspaceId(), currentUserId)
                .filter(m -> WorkspaceRole.CLIENT.name().equals(m.getRole()))
                .orElseThrow(() -> new AccessDeniedException("Chỉ CLIENT mới được approve gói nộp bài!"));

        pkg.setStatus(PackageStatus.APPROVED);
        pkg.setFeedback(request.getFeedback());
        pkg.setReviewedAt(LocalDateTime.now());

        return mapToPackageRespond(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public PackageRespond rejectPackage(ReviewPackageRequest request, UUID currentUserId) {
        log.info("User {} đang REJECT gói nộp bài {}", currentUserId, request.getPackageId());

        SubmissionPackage pkg = packageRepository.findByIdAndNotDeleted(request.getPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói nộp bài!"));

        if (!PackageStatus.PENDING.equals(pkg.getStatus())) {
            throw new IllegalStateException("Chỉ có thể review gói đang ở trạng thái PENDING!");
        }

        if (request.getFeedback() == null || request.getFeedback().isBlank()) {
            throw new IllegalArgumentException("Phải có feedback khi REJECT!");
        }

        // Chỉ CLIENT mới được reject
        memberRepository.findByWorkspaceIdAndUserId(pkg.getWorkspace().getWorkspaceId(), currentUserId)
                .filter(m -> WorkspaceRole.CLIENT.name().equals(m.getRole()))
                .orElseThrow(() -> new AccessDeniedException("Chỉ CLIENT mới được reject gói nộp bài!"));

        pkg.setStatus(PackageStatus.REJECTED);
        pkg.setFeedback(request.getFeedback());
        pkg.setReviewedAt(LocalDateTime.now());

        return mapToPackageRespond(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public void deletePackage(UUID packageId, UUID currentUserId) {
        log.info("User {} đang xóa gói nộp bài {}", currentUserId, packageId);

        SubmissionPackage pkg = packageRepository.findByIdAndNotDeleted(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói nộp bài!"));

        if (!PackageStatus.PENDING.equals(pkg.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xóa gói đang ở trạng thái PENDING!");
        }

        // Chỉ EXPERT (người nộp) mới được xóa
        memberRepository.findByWorkspaceIdAndUserId(pkg.getWorkspace().getWorkspaceId(), currentUserId)
                .filter(m -> WorkspaceRole.EXPERT.name().equals(m.getRole()))
                .orElseThrow(() -> new AccessDeniedException("Chỉ EXPERT mới được xóa gói nộp bài!"));

        pkg.setIsDelete(true);
        packageRepository.save(pkg);
    }

    @Override
    @Transactional
    public PackageRespond resubmitPackage(ResubmitPackageRequest request, UUID currentUserId) {

        // Lấy package cũ
        SubmissionPackage oldPkg = packageRepository.findByIdAndNotDeleted(request.getCurrentPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy package!"));

        // Chỉ EXPERT của workspace đó mới được resubmit
        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(oldPkg.getWorkspace().getWorkspaceId(), currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không thuộc Workspace này!"));

        if (!WorkspaceRole.EXPERT.equals(member.getRole())) {
            throw new AccessDeniedException("Chỉ EXPERT mới được nộp lại!");
        }

        // Chỉ cho resubmit khi package cũ đang PENDING hoặc REJECTED
        if (PackageStatus.APPROVED.equals(oldPkg.getStatus()) ||
                PackageStatus.SUPERSEDED.equals(oldPkg.getStatus())) {
            throw new IllegalStateException("Không thể nộp lại package đã APPROVED hoặc SUPERSEDED!");
        }

        // Đánh dấu package cũ là SUPERSEDED
        oldPkg.setStatus(PackageStatus.SUPERSEDED);
        oldPkg.getDeliverableFiles().forEach(f -> {
            if (!FileStatus.APPROVED.equals(f.getFileStatus())) {
                f.setFileStatus(FileStatus.SUPERSEDED);
            }
        });
        packageRepository.save(oldPkg);

        // Tạo package mới version tiếp theo
        int nextVersion = packageRepository
                .findMaxVersionByWorkspaceId(oldPkg.getWorkspace().getWorkspaceId())
                .orElse(0) + 1;

        SubmissionPackage newPkg = SubmissionPackage.builder()
                .workspace(oldPkg.getWorkspace())
                .version(nextVersion)
                .notes(request.getNotes())
                .autoReleaseAt(LocalDateTime.now().plusDays(7))
                .status(PackageStatus.PENDING)
                .isDelete(false)
                .build();

        // Map các fileType expert muốn update
        Map<FileType, DeliverableFileRequest> updatedMap = request.getUpdatedFiles().stream()
                .collect(Collectors.toMap(DeliverableFileRequest::fileType, f -> f));

        // Copy file APPROVED từ package cũ, thay thế file bị REJECTED/REVISION_REQUIRED/PENDING
        List<DeliverableFile> newFiles = oldPkg.getDeliverableFiles().stream()
                .map(oldFile -> {
                    if (FileStatus.APPROVED.equals(oldFile.getFileStatus())) {
                        // Giữ nguyên file đã APPROVED, copy sang package mới
                        return DeliverableFile.builder()
                                .submissionPackage(newPkg)
                                .fileName(oldFile.getFileName())
                                .fileUrl(oldFile.getFileUrl())
                                .fileSize(oldFile.getFileSize())
                                .fileType(oldFile.getFileType())
                                .fileStatus(FileStatus.APPROVED) // Giữ trạng thái APPROVED
                                .isDelete(false)
                                .build();
                    } else {
                        // Thay bằng file mới từ expert
                        DeliverableFileRequest newFile = updatedMap.get(oldFile.getFileType());
                        if (newFile == null) {
                            throw new IllegalArgumentException(
                                    "Thiếu file cho loại: " + oldFile.getFileType());
                        }
                        return DeliverableFile.builder()
                                .submissionPackage(newPkg)
                                .fileName(newFile.fileName())
                                .fileUrl(newFile.fileUrl())
                                .fileSize(newFile.fileSize())
                                .fileType(newFile.fileType())
                                .fileStatus(FileStatus.PENDING)
                                .isDelete(false)
                                .build();
                    }
                })
                .collect(Collectors.toList());

        newPkg.getDeliverableFiles().addAll(newFiles);
        SubmissionPackage saved = packageRepository.save(newPkg);
        UUID clientId = memberRepository
                .findByWorkspaceIdAndRole(oldPkg.getWorkspace().getWorkspaceId(), WorkspaceRole.CLIENT)
                .map(WorkspaceMember::getUserId)
                .orElse(null);

        if (clientId != null) {
            eventPublisher.publish(
                    NotificationEventPublisher.PACKAGE_RESUBMITTED,
                    NotificationEvent.builder()
                            .eventType("PACKAGE_RESUBMITTED")
                            .recipientUserId(clientId)
                            .senderName(member.getName())
                            .resourceId(saved.getPackageId())
                            .workspaceId(saved.getWorkspace().getWorkspaceId())
                            .resourceVersion(String.valueOf(saved.getVersion()))
                            .build()
            );
        }

        return mapToPackageRespond(packageRepository.save(newPkg));
    }

    /**
     * Phương thức validate dùng chung cho cả class
     */
    private void validateWorkspaceAccess(UUID workspaceId, UUID currentUserId) {
        memberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Quyền truy cập bị từ chối: Bạn không thuộc Workspace này!"));

        try {
            var userInfo = userClientService.getUserInfo(currentUserId.toString());
            if (userInfo == null) {
                throw new IllegalStateException("Người dùng không tồn tại trên hệ thống!");
            }
            log.info("User {} ({}) đang thực hiện thao tác với Workspace {}", currentUserId, userInfo.getUsername(), workspaceId);
        } catch (IdentityServiceUnavailableException e) {
            log.warn("⚠️ IdentityService không khả dụng. Fallback: Cho phép thao tác dựa trên xác thực DB nội bộ.");
        } catch (Exception e) {
            log.error("Lỗi xác thực gRPC cho user {}: {}", currentUserId, e.getMessage());
            throw new RuntimeException("Lỗi hệ thống xác thực!");
        }
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

    private void validateFiles(List<DeliverableFileRequest> files) {
        if (files == null || files.size() != FileType.values().length) {
            throw new IllegalArgumentException(
                    "Phải có đúng " + FileType.values().length + " file, mỗi loại 1 file!");
        }
        Set<FileType> types = files.stream()
                .map(DeliverableFileRequest::fileType)
                .collect(Collectors.toSet());
        if (types.size() != FileType.values().length) {
            throw new IllegalArgumentException("Mỗi loại FileType chỉ được upload 1 file!");
        }
    }
}
