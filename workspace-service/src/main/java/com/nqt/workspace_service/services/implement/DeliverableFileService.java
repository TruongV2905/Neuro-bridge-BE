package com.nqt.workspace_service.services.implement;

import com.nqt.common_starter.dto.event.NotificationEvent;
import com.nqt.common_starter.kafka.NotificationEventPublisher;
import com.nqt.workspace_service.dto.request.AddFileRequest;
import com.nqt.workspace_service.dto.request.ReviewFileRequest;
import com.nqt.workspace_service.dto.request.UpdateFileRequest;
import com.nqt.workspace_service.dto.response.DeliverableFileRespond;
import com.nqt.workspace_service.entities.DeliverableFile;
import com.nqt.workspace_service.entities.SubmissionPackage;
import com.nqt.workspace_service.entities.WorkspaceMember;
import com.nqt.workspace_service.enums.FileStatus;
import com.nqt.workspace_service.enums.PackageStatus;
import com.nqt.workspace_service.enums.WorkspaceRole;
import com.nqt.workspace_service.grpc.IdentityServiceUnavailableException;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repositories.DeliverableFileRepository;
import com.nqt.workspace_service.repositories.SubmissionPackageRepository;
import com.nqt.workspace_service.repositories.WorkspaceMemberRepository;
import com.nqt.workspace_service.services.interfaces.IDeliverableFileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
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
    private final WorkspaceMemberRepository memberRepository;
    private final SubmissionPackageRepository packageRepository;
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

        validateWorkspaceAccess(workspaceId, currentUserId);

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
        WorkspaceMember member = validateWorkspaceAccess(workspaceId, currentUserId);

        if ("EXPERT".equalsIgnoreCase(member.getRole().name())) { // Sửa lại m.getRole() tùy theo Enum của bạn
            throw new AccessDeniedException("Chuyên gia không nộp bài bằng luồng này!");
        }

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

        UUID workspaceId = file.getSubmissionPackage().getWorkspace().getWorkspaceId();
        WorkspaceMember member = validateWorkspaceAccess(workspaceId, currentUserId);

        if ("EXPERT".equalsIgnoreCase(member.getRole().name())) {
            throw new AccessDeniedException("Chuyên gia không có quyền chỉnh sửa file bàn giao của sinh viên!");
        }

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

        UUID workspaceId = file.getSubmissionPackage().getWorkspace().getWorkspaceId();
        WorkspaceMember member = validateWorkspaceAccess(workspaceId, currentUserId);

        if ("EXPERT".equalsIgnoreCase(member.getRole().name())) {
            throw new AccessDeniedException("Chuyên gia không có quyền gỡ tập tin sản phẩm của sinh viên!");
        }

        file.setIsDelete(true);
        fileRepository.save(file);
        return true;
    }

    @Override
    @Transactional
    public DeliverableFileRespond reviewFile(ReviewFileRequest req, UUID currentUserId) {
        DeliverableFile file = fileRepository.findById(req.fileId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file!"));

        // Chỉ CLIENT mới được review
        UUID workspaceId = file.getSubmissionPackage().getWorkspace().getWorkspaceId();
        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không thuộc Workspace này!"));

        if (!WorkspaceRole.CLIENT.equals(member.getRole())) {
            throw new AccessDeniedException("Chỉ CLIENT mới được review file!");
        }

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

        // Kiểm tra tự động update PackageStatus
        WorkspaceMember reviewer = memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                .orElseThrow();

        updatePackageStatusIfNeeded(file.getSubmissionPackage(),
                currentUserId,
                reviewer.getName());

        return mapToResponse(saved);
    }

    private void updatePackageStatusIfNeeded(SubmissionPackage pkg,
                                             UUID reviewerId,
                                             String reviewerName) {
        List<DeliverableFile> files = pkg.getDeliverableFiles().stream()
                .filter(f -> !Boolean.TRUE.equals(f.getIsDelete()))
                .toList();

        boolean allApproved = files.stream()
                .allMatch(f -> FileStatus.APPROVED.equals(f.getFileStatus()));

        boolean anyRejected = files.stream()
                .anyMatch(f -> FileStatus.REJECTED.equals(f.getFileStatus()) ||
                        FileStatus.REVISION_REQUIRED.equals(f.getFileStatus()));

        UUID workspaceId = pkg.getWorkspace().getWorkspaceId();

        if (allApproved) {
            pkg.setStatus(PackageStatus.APPROVED);
            packageRepository.save(pkg);

            // Lấy EXPERT để gửi thông báo
            UUID expertId = memberRepository
                    .findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.EXPERT)
                    .map(WorkspaceMember::getUserId)
                    .orElse(null);

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
            pkg.setStatus(PackageStatus.REJECTED);
            packageRepository.save(pkg);

            // Lấy EXPERT để gửi thông báo
            UUID expertId = memberRepository
                    .findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.EXPERT)
                    .map(WorkspaceMember::getUserId)
                    .orElse(null);

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

    private WorkspaceMember validateWorkspaceAccess(UUID workspaceId, UUID currentUserId) {
        // 1. Kiểm tra quyền thành viên trong DB (Quyền truy cập phòng)
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Quyền truy cập bị từ chối: Bạn không thuộc Workspace này!"));

        // 2. Kiểm tra danh tính qua gRPC (Cơ chế Fallback)
        try {
            var userInfo = userClientService.getUserInfo(currentUserId.toString());
            if (userInfo == null) {
                throw new IllegalStateException("Người dùng không tồn tại trên hệ thống!");
            }
            log.info("User {} (Name: {}) đang thực hiện thao tác trên Workspace {}", currentUserId, userInfo.getUsername(), workspaceId);
        } catch (IdentityServiceUnavailableException e) {
            log.warn("⚠️ IdentityService không khả dụng. Cho phép thao tác dựa trên xác thực DB.");
        } catch (Exception e) {
            log.error("Lỗi xác thực gRPC cho user {}: {}", currentUserId, e.getMessage());
            throw new RuntimeException("Lỗi hệ thống xác thực!");
        }

        return member;
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