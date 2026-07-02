package com.nqt.workspace_service.dto.event;
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
public class PackageNotificationEvent {
    private String eventType;
    private UUID packageId;
    private UUID workspaceId;
    private String packageVersion;
    private UUID recipientUserId;
    private String recipientName;
    private String senderName;
    private String notes;
    private String feedback;
    private LocalDateTime occurredAt;
}
