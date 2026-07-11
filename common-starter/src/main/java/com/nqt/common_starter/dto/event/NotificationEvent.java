package com.nqt.common_starter.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationEvent {

    // ── Loại event ───────────────────────────────────────────────
    private String eventType;        // PACKAGE_SUBMITTED, JOBPOST_APPLIED, ...
    private String domain;           // package, jobpost, workspace, payment, ...

    // ── Người liên quan ──────────────────────────────────────────
    private UUID recipientUserId;    // người NHẬN notification
    private String recipientName;    // tên người nhận (tuỳ chọn)
    private UUID senderUserId;       // userId người GỬI (tuỳ chọn)
    private String senderName;       // tên người gửi hiển thị trong message

    // ── Resource liên quan ───────────────────────────────────────
    private UUID resourceId;         // id chính (packageId, jobPostId, ...)
    private UUID workspaceId;        // workspaceId nếu có
    private String resourceVersion;  // version nếu có (v1, v2, ...)

    // ── Nội dung tuỳ chỉnh ───────────────────────────────────────
    private String customTitle;      // title tuỳ chỉnh (nếu không dùng default)
    private String customMessage;    // message tuỳ chỉnh (nếu không dùng default)
    private String customDeeplink;
    private String feedback;         // feedback khi REJECTED

    // ── Metadata ─────────────────────────────────────────────────
    private LocalDateTime occurredAt;
}
