package com.nqt.notification_service.dto.response;
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
public class NotificationPayload {
    private String type;
    private String title;
    private String message;
    private UUID resourceId;
    private UUID workspaceId;
    private String deeplink;
    private LocalDateTime timestamp;
}
