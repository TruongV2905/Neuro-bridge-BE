package com.nqt.notification_service.dto.response;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String type;
    private String title;
    private String message;
    private UUID resourceId;
    private UUID workspaceId;
    private boolean isRead;
    private LocalDateTime createdAt;
}
