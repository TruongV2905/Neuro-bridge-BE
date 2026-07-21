package com.nqt.notification_service.enums;

import com.nqt.common_starter.dto.event.NotificationEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public enum NotificationTemplate {

    PACKAGE_SUBMITTED(
            "Gói bàn giao mới",
            e -> String.format("%s đã nộp gói bàn giao v%s",
                    e.getSenderName(), e.getResourceVersion()),
            e -> String.format("/workspaces/%s/packages/%s", e.getWorkspaceId(), e.getResourceId())
    ),
    PACKAGE_RESUBMITTED(
            "Gói bàn giao đã được nộp lại",
            e -> String.format("%s đã nộp lại gói bàn giao v%s",
                    e.getSenderName(), e.getResourceVersion()),
            e -> String.format("/workspaces/%s/packages/%s", e.getWorkspaceId(), e.getResourceId())
    ),
    PACKAGE_APPROVED(
            "Gói bàn giao được chấp thuận ✓",
            e -> String.format("%s đã chấp thuận gói bàn giao v%s. " +
                            "Tiền trong ví chung đã được giải phóng vào tài khoản của bạn.",
                    e.getSenderName(), e.getResourceVersion()),
            e -> String.format("/workspaces/%s/packages/%s", e.getWorkspaceId(), e.getResourceId())
    ),
    PACKAGE_REJECTED(
            "Gói bàn giao bị từ chối",
            e -> String.format("%s đã từ chối gói bàn giao v%s: %s",
                    e.getSenderName(), e.getResourceVersion(), e.getFeedback()),
            e -> String.format("/workspaces/%s/packages/%s", e.getWorkspaceId(), e.getResourceId())
    ),
    WORKSPACE_INVITED(
            "Bạn đã được thêm vào Workspace",
            e -> "Neural Bridge đã tạo Workspace mới cho dự án của bạn. Hãy bắt đầu cộng tác!",
            e -> String.format("/workspaces/%s", e.getWorkspaceId())
    ),
    CONTRACT_SIGN_REQUIRED(
            "Hợp đồng cần được ký",
            e -> String.format("%s đã ký hợp đồng. Vui lòng vào ký để tiếp tục", e.getSenderName()),
            e -> String.format("/contracts/%s", e.getResourceId())
    ),
    CONTRACT_FULLY_SIGNED(
            "Hợp đồng đã được ký đầy đủ",
            e -> String.format("Hợp đồng với %s đã được ký đầy đủ. Vui lòng tiến hành thanh toán để bắt đầu dự án.",
                    e.getSenderName()),
            e -> String.format("/contracts/%s", e.getResourceId())
    ),
    JOBPOST_APPLIED(
            "Có Expert mới ứng tuyển",
            e -> String.format("%s đã ứng tuyển vào công việc của bạn", e.getSenderName()),
            e -> String.format("/job-posts/%s/applies", e.getResourceId())
    ),
    PROJECT_COMPLETED_PAYMENT_RELEASED(
            "Dự án đã hoàn tất",
            e -> String.format("Dự án \"%s\" đã hoàn tất. Số tiền dự án đã được chuyển vào ví của bạn.",
                    e.getSenderName()),
            e -> String.format("/workspaces/%s/projects/%s", e.getWorkspaceId(), e.getResourceId())
    ),
    UNKNOWN(
            "Thông báo mới",
            e -> "Có sự kiện mới trong hệ thống",
            e -> null
    );

    private final String title;
    private final Function<NotificationEvent, String> messageBuilder;
    private final Function<NotificationEvent, String> deeplinkBuilder;

    public String buildMessage(NotificationEvent event) {
        return messageBuilder.apply(event);
    }

    public String buildDeeplink(NotificationEvent event) {
        return deeplinkBuilder.apply(event);
    }

    public static NotificationTemplate fromEventType(String eventType) {
        try {
            return valueOf(eventType);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}