package com.nqt.workspace_service.services.implement;
import com.nqt.workspace_service.dto.request.ReviewPackageRequest;
import com.nqt.workspace_service.dto.response.SubmissionPackageResponse;
import com.nqt.workspace_service.entities.SubmissionPackage;
import com.nqt.workspace_service.entities.Workspace;
import com.nqt.workspace_service.enums.PackageStatus;
import com.nqt.workspace_service.enums.ProjectState;
import com.nqt.workspace_service.enums.WorkspaceStatus;
import com.nqt.workspace_service.grpc.IdentityServiceUnavailableException;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repositories.SubmissionPackageRepository;
import com.nqt.workspace_service.repositories.WorkspaceRepository;
import com.nqt.workspace_service.services.interfaces.IClientReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientReviewService implements IClientReviewService {

    private final SubmissionPackageRepository submissionPackageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserClientService userClientService;
    // private final KafkaTemplate<String, Object> kafkaTemplate; // Un-comment nếu team dùng Kafka

    /**
     * Nghiệp vụ 1: Nghiệm thu và Chấp nhận sản phẩm (Approve)
     */
    @Override
    @Transactional
    public SubmissionPackageResponse approvePackage(ReviewPackageRequest request, UUID clientId) {
        SubmissionPackage submissionPackage = submissionPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói sản phẩm này!"));

        Workspace workspace = submissionPackage.getWorkspace();
        validateClientAccess(workspace, clientId);

        // Cập nhật trạng thái Gói nộp
        submissionPackage.setStatus(PackageStatus.APPROVED);
        submissionPackage.setReviewedAt(LocalDateTime.now());
        if (request.getFeedback() != null) {
            submissionPackage.setFeedback(request.getFeedback());
        }

        // Cập nhật trạng thái Dự án và Workspace tổng thể
        workspace.getProjectDetail().setStatus(ProjectState.COMPLETED);
        workspace.getProjectDetail().setCompletedAt(LocalDateTime.now());
        workspace.getProjectDetail().setProgressPercentage(100.0f);
        workspace.setStatus(WorkspaceStatus.ARCHIVED); // Đóng băng phòng sau khi xong việc

        workspaceRepository.save(workspace);
        SubmissionPackage savedPackage = submissionPackageRepository.save(submissionPackage);

        // PHÁT EVENT SANG PAYMENT SERVICE ĐỂ GIẢI NGÂN (Event-Driven)
        log.info("Phòng làm việc {} hoàn thành. Phát event giải ngân ví Escrow: {}", workspace.getWorkspaceId(), workspace.getEscrowId());
        // =================================================================================
        // TODO 01: [BỎ NGỎ CHỜ PAYMENT SERVICE] - GIẢI NGÂN TIỀN VÍ ESCROW SANG CHO EXPERT
        // Khi team làm xong module Payment, mở đoạn Kafka Template dưới đây để kích hoạt giải ngân:
        // =================================================================================

        /*
        WorkspaceEvent event = WorkspaceEvent.builder()
                .eventType("WORKSPACE_COMPLETED")
                .workspaceId(workspace.getWorkspaceId())
                .escrowId(workspace.getEscrowId())
                .expertId(workspace.getExpert().getUserId())
                .build();

        kafkaTemplate.send("workspace-events", event);
        log.info("💰 Đã phát event HOÀN THÀNH sang Kafka topic [workspace-events] để lệnh giải ngân ví Escrow ID: {}", workspace.getEscrowId());
        */


        // =================================================================================
        // TODO 02: [BỎ NGỎ CHỜ NOTIFICATION SERVICE] - BẮN THÔNG BÁO CHÚC MỪNG HOÀN THÀNH DỰ ÁN
        // Khi team nạp lại module Notification, kích hoạt bắn tin nhắn Real-time thông báo cho cả 2 bên:
        // =================================================================================

        /*
        // 📢 2.1. Thông báo cho Expert (Đã được duyệt bài & nhận tiền)
        NotificationEvent expertNotice = NotificationEvent.builder()
                .recipientId(workspace.getExpert().getUserId())
                .title("🎉 ĐƠN HÀNG HOÀN THÀNH - SẢN PHẨM ĐÃ ĐƯỢC DUYỆT!")
                .content(String.format("Khách hàng đã duyệt gói sản phẩm của bạn tại phòng %s. Tiền công đang được chuyển vào ví của bạn.", workspace.getWorkspaceId()))
                .type("PROJECT_COMPLETED")
                .build();
        kafkaTemplate.send("notification-events", expertNotice);

        // 📢 2.2. Thông báo cho Client (Xác nhận đóng hồ sơ thành công)
        NotificationEvent clientNotice = NotificationEvent.builder()
                .recipientId(workspace.getClient().getUserId())
                .title("✅ BẠN ĐÃ DUYỆT SẢN PHẨM THÀNH CÔNG")
                .content(String.format("Dự án tại phòng %s đã chính thức khép lại. Cảm ơn bạn đã sử dụng dịch vụ!", workspace.getWorkspaceId()))
                .type("PROJECT_COMPLETED")
                .build();
        kafkaTemplate.send("notification-events", clientNotice);

        log.info("📢 Đã phát các event thông báo hoàn thành dự án sang Kafka topic [notification-events].");
        */

        return convertToResponseDto(savedPackage);
    }

    /**
     * Nghiệp vụ 2: Yêu cầu chỉnh sửa sản phẩm (Reject)
     */
    @Override
    @Transactional
    public SubmissionPackageResponse rejectPackage(ReviewPackageRequest request, UUID clientId) {
        if (request.getFeedback() == null || request.getFeedback().trim().isEmpty()) {
            throw new IllegalArgumentException("Bắt buộc phải nhập feedback chi tiết khi yêu cầu sửa đổi!");
        }

        SubmissionPackage submissionPackage = submissionPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói sản phẩm này!"));

        Workspace workspace = submissionPackage.getWorkspace();

        validateClientAccess(workspace, clientId);

        // Cập nhật trạng thái Gói nộp thành từ chối
        submissionPackage.setStatus(PackageStatus.REJECTED);
        submissionPackage.setReviewedAt(LocalDateTime.now());
        submissionPackage.setFeedback(request.getFeedback());

        // Chuyển trạng thái dự án về "Yêu cầu chỉnh sửa bản vá" để Expert biết đường làm tiếp
        workspace.getProjectDetail().setStatus(ProjectState.REVISION_REQUESTED);

        workspaceRepository.save(workspace);
        SubmissionPackage savedPackage = submissionPackageRepository.save(submissionPackage);

        return convertToResponseDto(savedPackage);
    }

    /**
     * Phương thức dùng chung để validate quyền truy cập bằng gRPC
     */
    private void validateClientAccess(Workspace workspace, UUID clientId) {
        // 1. Luôn kiểm tra quyền sở hữu từ Database nội bộ (Căn cứ xác thực chính)
        if (!workspace.getClientId().equals(clientId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải Client sở hữu phòng làm việc này!");
        }

        // 2. Cơ chế Fallback: Kiểm tra thông tin bổ sung từ gRPC
        try {
            var clientInfo = userClientService.getUserInfo(clientId.toString());

            if (clientInfo == null) {
                // Nếu gọi gRPC thành công mà trả về null -> User thực sự không tồn tại/bị xóa
                log.error("Client ID {} tồn tại trong DB Workspace nhưng không tìm thấy ở IdentityService!", clientId);
                throw new IllegalStateException("Tài khoản người dùng không hợp lệ!");
            }

            log.info("Client {} (ID: {}) thực hiện review sản phẩm.", clientInfo.getUsername(), clientId);

        } catch (IdentityServiceUnavailableException e) {
            log.warn("⚠️ IdentityService không khả dụng (gRPC Error: {}). Fallback: Cho phép tiếp tục dựa trên xác thực DB nội bộ.", e.getMessage());

        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi kiểm tra Client {} qua gRPC: {}", clientId, e.getMessage());
            throw new RuntimeException("Không thể xác thực thông tin người dùng bổ sung!");
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
