package com.nqt.workspace_service.service.implement;
import com.nqt.common_starter.dto.event.NotificationEvent;
import com.nqt.common_starter.kafka.NotificationEventPublisher;
import com.nqt.workspace_service.dto.request.DeliverableFileRequest;
import com.nqt.workspace_service.dto.request.ResubmitPackageRequest;
import com.nqt.workspace_service.dto.request.ReviewPackageRequest;
import com.nqt.workspace_service.dto.request.SubmitPackageRequest;
import com.nqt.workspace_service.dto.response.DeliverableFileRespond;
import com.nqt.workspace_service.dto.response.PackageResRespond;
import com.nqt.workspace_service.dto.response.PackageRespond;
import com.nqt.workspace_service.entity.DeliverableFile;
import com.nqt.workspace_service.entity.SubmissionPackage;
import com.nqt.workspace_service.entity.Workspace;
import com.nqt.workspace_service.constant.FileStatus;
import com.nqt.workspace_service.constant.FileType;
import com.nqt.workspace_service.constant.PackageStatus;
import com.nqt.workspace_service.grpc.IdentityServiceUnavailableException;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repository.SubmissionPackageRepository;
import com.nqt.workspace_service.repository.WorkspaceRepository;
import com.nqt.workspace_service.service.interfaces.ISubmissionPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserClientService userClientService;
//    private final PackageEventPublisher eventPublisher;
    private final NotificationEventPublisher eventPublisher;
    private final WorkspaceRepository workspaceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PackageRespond> getPackagesByWorkspace(
            UUID workspaceId,
            UUID currentUserId) {

        log.info(
                "User {} lấy package của Workspace {}",
                currentUserId,
                workspaceId
        );

        validateWorkspaceAccess(
                workspaceId,
                currentUserId,
                isAdmin()
        );

        return packageRepository
                .findByWorkspaceIdAndNotDeleted(workspaceId)
                .stream()
                .map(this::mapToPackageRespond)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PackageResRespond getPackageById(
            UUID packageId,
            UUID currentUserId) {

        SubmissionPackage pkg = packageRepository
                .findByIdAndNotDeleted(packageId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy package."));

        validateWorkspaceAccess(
                pkg.getWorkspace().getWorkspaceId(),
                currentUserId,
                isAdmin()
        );

        return mapToPackageResRespond(pkg);
    }

    @Override
    @Transactional
    public PackageRespond submitPackage(
            SubmitPackageRequest request,
            UUID currentUserId) {

        Workspace workspace = getWorkspace(request.getWorkspaceId());

        // Chỉ Expert của Workspace được submit
        validateExpert(workspace, currentUserId);

        // Không cho submit nếu đang có package PENDING
        boolean hasPending = packageRepository
                .findByWorkspaceIdAndNotDeleted(request.getWorkspaceId())
                .stream()
                .anyMatch(p -> PackageStatus.PENDING.equals(p.getStatus()));

        if (hasPending) {
            throw new IllegalStateException(
                    "Đang có package chờ review, không thể nộp thêm.");
        }

        // Validate đủ file
        validateFiles(request.getFiles());

        int nextVersion = packageRepository
                .findMaxVersionByWorkspaceId(request.getWorkspaceId())
                .orElse(0) + 1;

        int releaseDays =
                request.getCustomReleaseDays() == null
                        ? 7
                        : request.getCustomReleaseDays();

        SubmissionPackage pkg = SubmissionPackage.builder()
                .workspace(workspace)
                .version(nextVersion)
                .notes(request.getNotes())
                .status(PackageStatus.PENDING)
                .autoReleaseAt(LocalDateTime.now().plusDays(releaseDays))
                .isDelete(false)
                .build();

        List<DeliverableFile> files = request.getFiles()
                .stream()
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

        try {

            var sender = userClientService.getUserInfo(currentUserId.toString());

            eventPublisher.publish(
                    NotificationEventPublisher.PACKAGE_SUBMITTED,
                    NotificationEvent.builder()
                            .eventType("PACKAGE_SUBMITTED")
                            .recipientUserId(workspace.getClientId())
                            .senderName(sender.getFullName())
                            .resourceId(saved.getPackageId())
                            .workspaceId(workspace.getWorkspaceId())
                            .resourceVersion(String.valueOf(saved.getVersion()))
                            .build()
            );

        } catch (IdentityServiceUnavailableException ex) {

            log.warn("Identity Service unavailable, bỏ qua senderName.");

            eventPublisher.publish(
                    NotificationEventPublisher.PACKAGE_SUBMITTED,
                    NotificationEvent.builder()
                            .eventType("PACKAGE_SUBMITTED")
                            .recipientUserId(workspace.getClientId())
                            .resourceId(saved.getPackageId())
                            .workspaceId(workspace.getWorkspaceId())
                            .resourceVersion(String.valueOf(saved.getVersion()))
                            .build()
            );

        }

        return mapToPackageRespond(saved);
    }

    @Override
    @Transactional
    public PackageRespond approvePackage(
            ReviewPackageRequest request,
            UUID currentUserId) {

        log.info("User {} APPROVE package {}", currentUserId, request.getPackageId());

        SubmissionPackage pkg = packageRepository
                .findByIdAndNotDeleted(request.getPackageId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy gói nộp bài!"));

        if (!PackageStatus.PENDING.equals(pkg.getStatus())) {
            throw new IllegalStateException(
                    "Chỉ có thể review package đang ở trạng thái PENDING!");
        }

        Workspace workspace = getWorkspace(pkg.getWorkspace().getWorkspaceId());

        // Chỉ CLIENT của workspace mới được approve
        validateClient(workspace, currentUserId);

        pkg.setStatus(PackageStatus.APPROVED);
        pkg.setFeedback(request.getFeedback());
        pkg.setReviewedAt(LocalDateTime.now());

        return mapToPackageRespond(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public PackageRespond rejectPackage(
            ReviewPackageRequest request,
            UUID currentUserId) {

        log.info("User {} REJECT package {}", currentUserId, request.getPackageId());

        SubmissionPackage pkg = packageRepository
                .findByIdAndNotDeleted(request.getPackageId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy gói nộp bài!"));

        if (!PackageStatus.PENDING.equals(pkg.getStatus())) {
            throw new IllegalStateException(
                    "Chỉ có thể review package đang ở trạng thái PENDING!");
        }

        if (request.getFeedback() == null || request.getFeedback().isBlank()) {
            throw new IllegalArgumentException(
                    "Phải có feedback khi reject!");
        }

        Workspace workspace = getWorkspace(pkg.getWorkspace().getWorkspaceId());

        // Chỉ CLIENT mới được reject
        validateClient(workspace, currentUserId);

        pkg.setStatus(PackageStatus.REJECTED);
        pkg.setFeedback(request.getFeedback());
        pkg.setReviewedAt(LocalDateTime.now());

        return mapToPackageRespond(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public void deletePackage(
            UUID packageId,
            UUID currentUserId) {

        log.info("User {} DELETE package {}", currentUserId, packageId);

        SubmissionPackage pkg = packageRepository
                .findByIdAndNotDeleted(packageId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy package!"));

        if (!PackageStatus.PENDING.equals(pkg.getStatus())) {
            throw new IllegalStateException(
                    "Chỉ có thể xóa package đang ở trạng thái PENDING!");
        }

        Workspace workspace = getWorkspace(pkg.getWorkspace().getWorkspaceId());

        // Chỉ EXPERT mới được xóa
        validateExpert(workspace, currentUserId);

        pkg.setIsDelete(true);

        packageRepository.save(pkg);
    }

    @Override
    @Transactional
    public PackageRespond resubmitPackage(
            ResubmitPackageRequest request,
            UUID currentUserId) {

        // Package cũ
        SubmissionPackage oldPkg = packageRepository
                .findByIdAndNotDeleted(request.getCurrentPackageId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy package!"));

        Workspace workspace = getWorkspace(
                oldPkg.getWorkspace().getWorkspaceId());

        // Chỉ Expert mới được resubmit
        validateExpert(workspace, currentUserId);

        // Không cho sửa file đã APPROVED
        Set<FileType> approvedTypes = oldPkg.getDeliverableFiles()
                .stream()
                .filter(f -> FileStatus.APPROVED.equals(f.getFileStatus()))
                .map(DeliverableFile::getFileType)
                .collect(Collectors.toSet());

        List<FileType> invalidUpdates = request.getUpdatedFiles()
                .stream()
                .map(DeliverableFileRequest::fileType)
                .filter(approvedTypes::contains)
                .collect(Collectors.toList());

        if (!invalidUpdates.isEmpty()) {
            throw new IllegalArgumentException(
                    "Không thể thay đổi file đã APPROVED: "
                            + invalidUpdates);
        }

        // Chỉ resubmit khi package chưa APPROVED
        if (PackageStatus.APPROVED.equals(oldPkg.getStatus())
                || PackageStatus.SUPERSEDED.equals(oldPkg.getStatus())) {

            throw new IllegalStateException(
                    "Không thể nộp lại package đã APPROVED hoặc SUPERSEDED!");
        }

        // Package cũ -> SUPERSEDED
        oldPkg.setStatus(PackageStatus.SUPERSEDED);

        oldPkg.getDeliverableFiles().forEach(file -> {

            if (!FileStatus.APPROVED.equals(file.getFileStatus())) {
                file.setFileStatus(FileStatus.SUPERSEDED);
            }

        });

        packageRepository.save(oldPkg);

        // Version mới
        int nextVersion = packageRepository
                .findMaxVersionByWorkspaceId(workspace.getWorkspaceId())
                .orElse(0) + 1;

        SubmissionPackage newPkg = SubmissionPackage.builder()
                .workspace(workspace)
                .version(nextVersion)
                .notes(request.getNotes())
                .status(PackageStatus.PENDING)
                .autoReleaseAt(LocalDateTime.now().plusDays(7))
                .isDelete(false)
                .build();

        // Các file expert upload mới
        Map<FileType, DeliverableFileRequest> updatedMap =
                request.getUpdatedFiles()
                        .stream()
                        .collect(Collectors.toMap(
                                DeliverableFileRequest::fileType,
                                f -> f
                        ));

        List<DeliverableFile> files =
                oldPkg.getDeliverableFiles()
                        .stream()
                        .map(oldFile -> {

                            if (FileStatus.APPROVED.equals(oldFile.getFileStatus())) {

                                return DeliverableFile.builder()
                                        .submissionPackage(newPkg)
                                        .fileName(oldFile.getFileName())
                                        .fileUrl(oldFile.getFileUrl())
                                        .fileSize(oldFile.getFileSize())
                                        .fileType(oldFile.getFileType())
                                        .fileStatus(FileStatus.APPROVED)
                                        .isDelete(false)
                                        .build();
                            }

                            DeliverableFileRequest updated =
                                    updatedMap.get(oldFile.getFileType());

                            if (updated == null) {
                                throw new IllegalArgumentException(
                                        "Thiếu file cho loại: "
                                                + oldFile.getFileType());
                            }

                            return DeliverableFile.builder()
                                    .submissionPackage(newPkg)
                                    .fileName(updated.fileName())
                                    .fileUrl(updated.fileUrl())
                                    .fileSize(updated.fileSize())
                                    .fileType(updated.fileType())
                                    .fileStatus(FileStatus.PENDING)
                                    .isDelete(false)
                                    .build();

                        })
                        .collect(Collectors.toList());

        newPkg.getDeliverableFiles().addAll(files);

        SubmissionPackage saved = packageRepository.save(newPkg);

        // Notification
        try {

            var sender =
                    userClientService.getUserInfo(currentUserId.toString());

            eventPublisher.publish(
                    NotificationEventPublisher.PACKAGE_RESUBMITTED,
                    NotificationEvent.builder()
                            .eventType("PACKAGE_RESUBMITTED")
                            .recipientUserId(workspace.getClientId())
                            .senderName(sender.getFullName())
                            .resourceId(saved.getPackageId())
                            .workspaceId(workspace.getWorkspaceId())
                            .resourceVersion(
                                    String.valueOf(saved.getVersion()))
                            .build()
            );

        } catch (IdentityServiceUnavailableException ex) {

            log.warn("Identity Service unavailable.");

            eventPublisher.publish(
                    NotificationEventPublisher.PACKAGE_RESUBMITTED,
                    NotificationEvent.builder()
                            .eventType("PACKAGE_RESUBMITTED")
                            .recipientUserId(workspace.getClientId())
                            .resourceId(saved.getPackageId())
                            .workspaceId(workspace.getWorkspaceId())
                            .resourceVersion(
                                    String.valueOf(saved.getVersion()))
                            .build()
            );

        }

        return mapToPackageRespond(saved);
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

    private Workspace getWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy Workspace!"));
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
}
