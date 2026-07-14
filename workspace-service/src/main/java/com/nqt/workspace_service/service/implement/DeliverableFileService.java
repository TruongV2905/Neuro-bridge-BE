package com.nqt.workspace_service.service.implement;

import com.nqt.common_starter.dto.event.NotificationEvent;
import com.nqt.common_starter.kafka.NotificationEventPublisher;
import com.nqt.workspace_service.dto.request.AddFileRequest;
import com.nqt.workspace_service.dto.request.ReviewFileRequest;
import com.nqt.workspace_service.dto.request.UpdateFileRequest;
import com.nqt.workspace_service.dto.response.DeliverableFileRespond;
import com.nqt.workspace_service.entity.DeliverableFile;
import com.nqt.workspace_service.entity.SubmissionPackage;
import com.nqt.workspace_service.entity.Workspace;
import com.nqt.workspace_service.constant.FileStatus;
import com.nqt.workspace_service.constant.PackageStatus;
import com.nqt.workspace_service.grpc.IdentityServiceUnavailableException;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repository.DeliverableFileRepository;
import com.nqt.workspace_service.repository.SubmissionPackageRepository;
import com.nqt.workspace_service.repository.WorkspaceRepository;
import com.nqt.workspace_service.service.interfaces.IDeliverableFileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliverableFileService implements IDeliverableFileService {

    private final DeliverableFileRepository fileRepository;
    private final SubmissionPackageRepository packageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserClientService userClientService;
    private final NotificationEventPublisher eventPublisher;

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<DeliverableFileRespond> getFilesByPackage(UUID packageId, UUID currentUserId) {
        log.info("User {} đang yêu cầu xem danh sách file của Package {}", currentUserId, packageId);

        UUID workspaceId = fileRepository.findWorkspaceIdByPackageId(packageId);
        if (workspaceId == null) {
            throw new IllegalArgumentException("Không tìm thấy gói nộp bài này!");
        }

        validateWorkspaceAccess(workspaceId, currentUserId, isAdmin());

        return fileRepository.findByPackageIdAndNotDeleted(packageId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DeliverableFileRespond addFile(AddFileRequest req, UUID currentUserId) {
        log.info("User {} đang thêm file vào Package {}", currentUserId, req.packageId());

        UUID workspaceId = fileRepository.findWorkspaceIdByPackageId(req.packageId());
        if (workspaceId == null) {
            throw new IllegalArgumentException("Gói nộp bài không tồn tại!");
        }

        Workspace workspace = getWorkspace(workspaceId);

        // Chỉ EXPERT (hoặc ADMIN) được thêm file
        validateExpert(workspace, currentUserId);

        SubmissionPackage submissionPackage = entityManager.getReference(SubmissionPackage.class, req.packageId());

        DeliverableFile file = DeliverableFile.builder()
                .submissionPackage(submissionPackage)
                .fileName(req.fileName())
                .fileUrl(req.fileUrl())
                .fileSize(req.fileSize())
                .fileType(req.fileType())
                .isDelete(false)
                .build();

        DeliverableFile savedFile = fileRepository.save(file);
        return mapToResponse(savedFile);
    }

    @Override
    @Transactional
    public DeliverableFileRespond updateFile(UpdateFileRequest req, UUID currentUserId) {
        log.info("User {} đang chỉnh sửa file ID {}", currentUserId, req.fileId());

        DeliverableFile file = fileRepository.findById(req.fileId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file yêu cầu!"));

        if (Boolean.TRUE.equals(file.getIsDelete())) {
            throw new IllegalArgumentException("File này đã bị xóa trước đó, không thể chỉnh sửa.");
        }

        Workspace workspace = file.getSubmissionPackage().getWorkspace();

        // Chỉ EXPERT (hoặc ADMIN) mới có quyền chỉnh sửa file bàn giao
        validateExpert(workspace, currentUserId);

        file.setFileName(req.fileName());
        file.setFileUrl(req.fileUrl());
        file.setFileSize(req.fileSize());
        file.setFileType(req.fileType());
        file.setUploadedAt(LocalDateTime.now());

        DeliverableFile updatedFile = fileRepository.save(file);
        return mapToResponse(updatedFile);
    }

    @Override
    @Transactional
    public boolean deleteFile(UUID fileId, UUID currentUserId) {
        log.info("User {} đang thực hiện gỡ file ID {}", currentUserId, fileId);

        DeliverableFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file yêu cầu!"));

        Workspace workspace = file.getSubmissionPackage().getWorkspace();

        // Chỉ EXPERT (hoặc ADMIN) mới có quyền gỡ file
        validateExpert(workspace, currentUserId);

        file.setIsDelete(true);
        fileRepository.save(file);
        return true;
    }

    @Override
    @Transactional
    public DeliverableFileRespond reviewFile(ReviewFileRequest req, UUID currentUserId) {
        DeliverableFile file = fileRepository.findById(req.fileId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file!"));

        Workspace workspace = file.getSubmissionPackage().getWorkspace();

        // Chỉ CLIENT (hoặc ADMIN) mới được review
        validateClient(workspace, currentUserId);

        // Không cho review file đã APPROVED hoặc SUPERSEDED
        if (FileStatus.APPROVED.equals(file.getFileStatus()) ||
                FileStatus.SUPERSEDED.equals(file.getFileStatus())) {
            throw new IllegalStateException("File này không thể review nữa!");
        }

        // Bắt buộc có feedback khi REJECTED hoặc REVISION_REQUIRED
        if ((FileStatus.REJECTED.equals(req.newStatus()) ||
                FileStatus.REVISION_REQUIRED.equals(req.newStatus()))
                && (req.feedback() == null || req.feedback().isBlank())) {
            throw new IllegalArgumentException("Phải có feedback khi REJECTED hoặc yêu cầu chỉnh sửa!");
        }

        file.setFileStatus(req.newStatus());
        file.setFeedback(req.feedback());
        DeliverableFile saved = fileRepository.save(file);

        String reviewerName = resolveUserName(currentUserId);

        updatePackageStatusIfNeeded(file.getSubmissionPackage(), reviewerName);

        return mapToResponse(saved);
    }

    private void updatePackageStatusIfNeeded(SubmissionPackage pkg, String reviewerName) {
        List<DeliverableFile> files = pkg.getDeliverableFiles().stream()
                .filter(f -> !Boolean.TRUE.equals(f.getIsDelete()))
                .toList();

        boolean allApproved = files.stream()
                .allMatch(f -> FileStatus.APPROVED.equals(f.getFileStatus()));

        boolean anyRejected = files.stream()
                .anyMatch(f -> FileStatus.REJECTED.equals(f.getFileStatus()) ||
                        FileStatus.REVISION_REQUIRED.equals(f.getFileStatus()));

        Workspace workspace = pkg.getWorkspace();
        UUID workspaceId = workspace.getWorkspaceId();
        UUID expertId = workspace.getExpertId();

        if (allApproved) {
            if (PackageStatus.APPROVED.equals(pkg.getStatus())) {
                return;
            }
            pkg.setStatus(PackageStatus.APPROVED);
            packageRepository.save(pkg);

            if (expertId != null) {
                eventPublisher.publish(
                        NotificationEventPublisher.PACKAGE_APPROVED,
                        NotificationEvent.builder()
                                .eventType("PACKAGE_APPROVED")
                                .recipientUserId(expertId)
                                .senderName(reviewerName)
                                .resourceId(pkg.getPackageId())
                                .workspaceId(workspaceId)
                                .resourceVersion(String.valueOf(pkg.getVersion()))
                                // Thêm message tiền được release
                                .customMessage(String.format(
                                        "%s đã chấp thuận toàn bộ gói bàn giao v%s. " +
                                                "Tiền trong ví chung đã được giải phóng vào tài khoản của bạn.",
                                        reviewerName, pkg.getVersion()))
                                .build()
                );
            }

        } else if (anyRejected) {
            // ⚠️ Tránh spam: nếu package đã REJECTED rồi thì không set/notify lại
            if (PackageStatus.REJECTED.equals(pkg.getStatus())) {
                return;
            }
            pkg.setStatus(PackageStatus.REJECTED);
            packageRepository.save(pkg);

            if (expertId != null) {
                // Gom feedback của các file bị reject
                String feedback = files.stream()
                        .filter(f -> FileStatus.REJECTED.equals(f.getFileStatus()) ||
                                FileStatus.REVISION_REQUIRED.equals(f.getFileStatus()))
                        .map(f -> f.getFileType() + ": " + f.getFeedback())
                        .reduce("", (a, b) -> a.isEmpty() ? b : a + " | " + b);

                eventPublisher.publish(
                        NotificationEventPublisher.PACKAGE_REJECTED,
                        NotificationEvent.builder()
                                .eventType("PACKAGE_REJECTED")
                                .recipientUserId(expertId)
                                .senderName(reviewerName)
                                .resourceId(pkg.getPackageId())
                                .workspaceId(workspaceId)
                                .resourceVersion(String.valueOf(pkg.getVersion()))
                                .feedback(feedback)
                                .build()
                );
            }
        }
    }

    private String resolveUserName(UUID userId) {
        try {
            return userClientService.getUserInfo(userId.toString()).getFullName();
        } catch (IdentityServiceUnavailableException e) {
            log.warn("Identity Service unavailable, bỏ qua senderName.");
            return null;
        }
    }

    private Workspace getWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Workspace!"));
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void validateExpert(Workspace workspace, UUID currentUserId) {

        if (isAdmin()) {
            return;
        }

        if (!currentUserId.equals(workspace.getExpertId())) {
            throw new AccessDeniedException(
                    "Chỉ EXPERT của Workspace mới được thực hiện thao tác này!");
        }
    }

    private void validateClient(Workspace workspace, UUID currentUserId) {

        if (isAdmin()) {
            return;
        }

        if (!currentUserId.equals(workspace.getClientId())) {
            throw new AccessDeniedException(
                    "Chỉ CLIENT của Workspace mới được thực hiện thao tác này!");
        }
    }

    private void validateWorkspaceAccess(
            UUID workspaceId,
            UUID currentUserId,
            boolean isAdmin) {

        Workspace workspace = getWorkspace(workspaceId);

        if (!isAdmin
                && !currentUserId.equals(workspace.getClientId())
                && !currentUserId.equals(workspace.getExpertId())) {

            throw new AccessDeniedException(
                    "Quyền truy cập bị từ chối: Bạn không thuộc Workspace này!");
        }

        try {

            var userInfo = userClientService.getUserInfo(currentUserId.toString());

            if (userInfo == null) {
                throw new IllegalStateException("Người dùng không tồn tại!");
            }

            log.info(
                    "User {} ({}) đang thao tác Workspace {}",
                    currentUserId,
                    userInfo.getFullName(),
                    workspaceId
            );

        } catch (IdentityServiceUnavailableException e) {

            log.warn("Identity Service unavailable. Skip user info.");

        } catch (Exception e) {

            log.error("Identity Error", e);
            throw new RuntimeException("Lỗi hệ thống xác thực.");

        }
    }

    private DeliverableFileRespond mapToResponse(DeliverableFile file) {
        return new DeliverableFileRespond(
                file.getFileId(),
                file.getSubmissionPackage() != null ? file.getSubmissionPackage().getPackageId() : null,
                file.getFileName(),
                file.getFileUrl(),
                file.getFileSize(),
                file.getFileType(),
                file.getUploadedAt(),
                file.getIsDelete()
        );
    }
}