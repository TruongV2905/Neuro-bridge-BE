package com.nqt.workspace_service.service.implement;

import com.nqt.workspace_service.dto.request.DisputeWorkspaceRequest;
import com.nqt.workspace_service.entity.SubmissionPackage;
import com.nqt.workspace_service.entity.Workspace;
import com.nqt.workspace_service.constant.PackageStatus;
import com.nqt.workspace_service.constant.ProjectState;
import com.nqt.workspace_service.constant.WorkspaceStatus;
import com.nqt.workspace_service.grpc.UserClientService;
import com.nqt.workspace_service.repository.SubmissionPackageRepository;
import com.nqt.workspace_service.repository.WorkspaceRepository;
import com.nqt.workspace_service.service.interfaces.IWorkspaceOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceOrchestrationService implements IWorkspaceOrchestrationService {

    private final SubmissionPackageRepository submissionPackageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserClientService userClientService;
    /**
     * Nghiệp vụ 1: Tự động nghiệm thu (Cron Job chạy ngầm)
     * Quét định kỳ mỗi tiếng một lần (hoặc tùy chỉnh bằng cron expression)
     */
    @Override
    @Scheduled(cron = "0 0 * * * *") // Chạy vào đầu mỗi giờ
    @Transactional
    public void triggerAutoRelease() {
        log.info("Background Job: Bắt đầu quét các gói sản phẩm quá hạn Client phản hồi...");

        List<SubmissionPackage> expiredPackages = submissionPackageRepository
                .findByStatusAndAutoReleaseAtBefore(PackageStatus.PENDING, LocalDateTime.now());

        if (expiredPackages.isEmpty()) {
            log.info("Background Job: Không có gói sản phẩm nào quá hạn.");
            return;
        }

        for (SubmissionPackage sp : expiredPackages) {
            log.info("Phát hiện gói nộp bài ID {} thuộc Workspace {} bị ngâm quá 3 ngày. Tiến hành auto-approve.",
                    sp.getPackageId(), sp.getWorkspace().getWorkspaceId());

            Workspace workspace = sp.getWorkspace();

            // Đổi trạng thái gói nộp bài sang APPROVED
            sp.setStatus(PackageStatus.APPROVED);
            sp.setReviewedAt(LocalDateTime.now());
            sp.setFeedback("Hệ thống tự động nghiệm thu do Client không phản hồi sau 3 ngày.");

            // Đổi trạng thái tiến độ dự án
            workspace.getProjectDetail().setStatus(ProjectState.COMPLETED);
            workspace.getProjectDetail().setCompletedAt(LocalDateTime.now());
            workspace.getProjectDetail().setProgressPercentage(100.0f);
            workspace.setStatus(WorkspaceStatus.ARCHIVED); // Đóng băng phòng luôn

            workspaceRepository.save(workspace);
            submissionPackageRepository.save(sp);

            // =================================================================================
            // TODO 01: [BỎ NGỎ CHỜ PAYMENT SERVICE] - TỰ ĐỘNG GIẢI NGÂN TIỀN VÍ ESCROW CHO EXPERT
            // =================================================================================
            /*
            WorkspaceEvent event = WorkspaceEvent.builder()
                    .eventType("WORKSPACE_COMPLETED")
                    .workspaceId(workspace.getWorkspaceId())
                    .escrowId(workspace.getEscrowId())
                    .expertId(workspace.getExpert().getUserId())
                    .build();
            kafkaTemplate.send("workspace-events", event);
            log.info("💰 [Auto] Đã phát event GIẢI NGÂN sang Kafka topic [workspace-events] cho Workspace: {}", workspace.getWorkspaceId());
            */

            // =================================================================================
            // TODO 02: [BỎ NGỎ CHỜ NOTIFICATION SERVICE] - BẮN THÔNG BÁO TỰ ĐỘNG NGHIỆM THU
            // =================================================================================
            /*
            NotificationEvent expertNotice = NotificationEvent.builder()
                    .recipientId(workspace.getExpert().getUserId())
                    .title("🎉 ĐƠN HÀNG TỰ ĐỘNG HOÀN THÀNH!")
                    .content(String.format("Sản phẩm tại phòng %s đã tự động thông qua do quá hạn phản hồi. Tiền công đang được chuyển về ví của bạn.", workspace.getWorkspaceId()))
                    .type("PROJECT_AUTO_COMPLETED")
                    .build();

            NotificationEvent clientNotice = NotificationEvent.builder()
                    .recipientId(workspace.getClient().getUserId())
                    .title("📦 HỆ THỐNG TỰ ĐỘNG NGHIỆM THU")
                    .content(String.format("Hệ thống đã tự động đóng hồ sơ dự án %s do bạn không phản hồi quá thời hạn.", workspace.getWorkspaceId()))
                    .type("PROJECT_AUTO_COMPLETED")
                    .build();

            kafkaTemplate.send("notification-events", expertNotice);
            kafkaTemplate.send("notification-events", clientNotice);
            */

            // =================================================================================
            // TODO 03: [BỎ NGỎ CHỜ CHAT SERVICE] - ĐỔI TRẠNG THÁI CHAT SESSION SANG 'CLOSED'
            // =================================================================================
            /*
            WorkspaceChatControlEvent chatCloseEvent = WorkspaceChatControlEvent.builder()
                    .eventType("CHAT_ROOM_CLOSE")
                    .workspaceId(workspace.getWorkspaceId())
                    .build();
            kafkaTemplate.send("chat-control-events", chatCloseEvent);
            log.info("🔒 [Auto] Đã phát lệnh ĐÓNG ROOM CHAT (CLOSED) sang Kafka topic [chat-control-events]");
            */
        }
    }

    /**
     * Nghiệp vụ 2: Kích hoạt Tranh chấp (Disputed)
     */
    @Override
    @Transactional
    public void disputeWorkspace(DisputeWorkspaceRequest request, UUID requesterId, boolean isAdmin) {
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Không thể mở tranh chấp nếu thiếu lý do cụ thể!");
        }
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy không gian làm việc này!"));

        // Kiểm tra quyền: Chỉ Client, Expert trong phòng hoặc Admin hệ thống mới có quyền tố cáo/tranh chấp
        if (!isAdmin && !workspace.getClientId().equals(requesterId)
                && !workspace.getClientId().equals(requesterId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền can thiệp vào phòng làm việc này!");
        }

        enrichDisputeLog(requesterId, workspace.getWorkspaceId(), request.getReason());
        // Chuyển trạng thái dự án sang DISPUTED (Đóng băng tạm thời để Admin vào can thiệp)
        workspace.getProjectDetail().setStatus(ProjectState.DISPUTED);

        log.warn("Workspace {} đã chuyển sang trạng thái TRANH CHẤP bởi Người dùng ID: {}. Lý do: {}",
                workspace.getWorkspaceId(), requesterId, request.getReason());

        workspaceRepository.save(workspace);

        // =================================================================================
        // TODO 01: [BỎ NGỎ CHỜ PAYMENT SERVICE] - PHONG TỎA DÒNG TIỀN VÍ ESCROW KHI XẢY RA TRANH CHẤP
        // Sau khi team hoàn thiện module Payment, lựa chọn 1 trong 2 cách kết nối dưới đây:
        // =================================================================================

        /*
        WorkspaceEvent disputeEvent = WorkspaceEvent.builder()
                .eventType("WORKSPACE_DISPUTED")
                .workspaceId(workspace.getWorkspaceId())
                .escrowId(workspace.getEscrowId())
                .requesterId(requesterId)
                .reason(request.getReason())
                .build();

        kafkaTemplate.send("workspace-events", disputeEvent);
        log.info("💥 Đã phát event TRANH CHẤP sang Kafka topic [workspace-events] để đóng băng ví Escrow ID: {}", workspace.getEscrowId());
        */


        // =================================================================================
        // TODO 02: [BỎ NGỎ CHỜ NOTIFICATION SERVICE] - BẮN THÔNG BÁO KHẨN CẤP CHO ADMIN
        // Khi team nạp lại module Notification, dùng Kafka để bắn tin nhắn bất đồng bộ:
        // =================================================================================

        /*
        NotificationEvent adminAlert = NotificationEvent.builder()
                .recipientRole("ADMIN") // Gửi đích danh cho nhóm quyền Admin
                .title("🚨 PHÒNG LÀM VIỆC PHÁT SINH TRANH CHẤP CẤP CỨU!")
                .content(String.format("Phòng %s vừa bị khiếu nại bởi Người dùng %s. Lý do: %s",
                        workspace.getWorkspaceId(), requesterId, request.getReason()))
                .type("SYSTEM_ALERT")
                .build();

        kafkaTemplate.send("notification-events", adminAlert);
        log.info("📢 Đã phát event thông báo tranh chấp sang Kafka topic [notification-events] để cảnh báo Admin.");
        */

        // =================================================================================
        // TODO 03: [BỎ NGỎ CHỜ CHAT SERVICE] - ĐỔI TRẠNG THÁI CHAT SESSION SANG 'BLOCKED'
        // Đóng băng tạm thời phòng chat khi xảy ra tranh chấp để giữ nguyên hiện trường
        // =================================================================================
        /*
        WorkspaceChatControlEvent chatBlockEvent = WorkspaceChatControlEvent.builder()
                .eventType("CHAT_ROOM_BLOCK") // Tín hiệu báo hiệu chặn/đóng băng tạm thời phòng chat
                .workspaceId(workspace.getWorkspaceId())
                .build();

        kafkaTemplate.send("chat-control-events", chatBlockEvent);
        log.info("🚫 Đã phát event CHẶN CHAT (BLOCKED) sang Kafka topic [chat-control-events] cho Workspace: {}", workspace.getWorkspaceId());
        */
    }

    /**
     * Phương thức tranh chấp bằng gRPC
     */
    private void enrichDisputeLog(UUID requesterId, UUID workspaceId, String reason) {
        try {
            var userInfo = userClientService.getUserInfo(requesterId.toString());
            String name = (userInfo != null) ? userInfo.getFullName() : "Unknown";
            log.warn("🚨 TRANH CHẤP Workspace {} bởi {} (ID: {}). Lý do: {}",
                    workspaceId, name, requesterId, reason);
        } catch (Exception e) {
            log.warn("🚨 TRANH CHẤP Workspace {} bởi ID: {}. Lý do: {} (GRPC unavailable).",
                    workspaceId, requesterId, reason);
        }
    }

    /**
     * Nghiệp vụ 3: Lưu trữ / Đóng băng phòng làm việc (Archive)
     */
    @Override
    @Transactional
    public void archiveWorkspace(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy không gian làm việc này!"));

        // Điều kiện để được đóng băng chủ động: Dự án phải ở trạng thái hoàn thành (COMPLETED)
        if (!workspace.getProjectDetail().getStatus().equals(ProjectState.COMPLETED)) {
            throw new IllegalStateException("Không thể lưu trữ không gian làm việc chưa hoàn thành!");
        }

        workspace.setStatus(WorkspaceStatus.ARCHIVED);
        log.info("Không gian làm việc {} đã chính thức đóng băng (ARCHIVED) thành lịch sử.", workspaceId);

        workspaceRepository.save(workspace);

        // =================================================================================
        // TODO 01: [BỎ NGỎ CHỜ NOTIFICATION SERVICE] - BẮN THÔNG BÁO CHO USER PHÒNG ĐÃ BỊ LƯU TRỮ
        // Khi team nạp lại module Notification, kích hoạt bắn tin nhắn gửi cho cả 2 bên:
        // =================================================================================
        /*
        // 📢 1.1. Gửi thông báo cho phía Expert
        NotificationEvent archiveNoticeForExpert = NotificationEvent.builder()
                .recipientId(workspace.getExpert().getUserId())
                .title("📦 PHÒNG LÀM VIỆC ĐÃ ĐƯỢC LƯU TRỮ")
                .content(String.format("Hệ thống đã chuyển phòng %s vào mục Lưu trữ lịch sử.", workspace.getWorkspaceId()))
                .type("WORKSPACE_ARCHIVED")
                .build();

        // 📢 1.2. Gửi thông báo cho phía Client
        NotificationEvent archiveNoticeForClient = NotificationEvent.builder()
                .recipientId(workspace.getClient().getUserId())
                .title("📦 PHÒNG LÀM VIỆC ĐÃ ĐƯỢC LƯU TRỮ")
                .content(String.format("Hệ thống đã chuyển phòng %s vào mục Lưu trữ lịch sử.", workspace.getWorkspaceId()))
                .type("WORKSPACE_ARCHIVED")
                .build();

        kafkaTemplate.send("notification-events", archiveNoticeForExpert);
        kafkaTemplate.send("notification-events", archiveNoticeForClient);
        log.info("📢 Đã phát các event thông báo lưu trữ phòng sang Kafka topic [notification-events].");
        */


        // =================================================================================
        // TODO 02: [BỎ NGỎ CHỜ CHAT SERVICE] - ĐỔI TRẠNG THÁI CHAT SESSION SANG 'CLOSED'
        // Đồng bộ trạng thái mạng nội bộ, ra lệnh khóa quyền gửi tin nhắn của Room chat này
        // =================================================================================
        /*
        WorkspaceChatControlEvent chatCloseEvent = WorkspaceChatControlEvent.builder()
                .eventType("CHAT_ROOM_CLOSE") // Tín hiệu kích hoạt đổi ChatSessionStatus sang CLOSED
                .workspaceId(workspace.getWorkspaceId())
                .build();

        kafkaTemplate.send("chat-control-events", chatCloseEvent);
        log.info("🔒 Đã phát event KHÓA CHAT (CLOSED) sang Kafka topic [chat-control-events] cho Workspace: {}", workspace.getWorkspaceId());
        */
    }
}
