package com.nqt.notification_service.service;

import com.nqt.common_starter.dto.event.NotificationEvent;
import com.nqt.notification_service.dto.response.NotificationPayload;
import com.nqt.notification_service.dto.response.NotificationResponse;
import com.nqt.notification_service.entity.Notification;
import com.nqt.notification_service.enums.NotificationTemplate;
import com.nqt.notification_service.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final NotificationCacheService cacheService;

    @Transactional
    public void sendNotification(NotificationEvent event) {
        NotificationPayload payload = buildPayload(event);

        // 1. Lưu vào DB
        Notification notification = Notification.builder()
                .recipientUserId(event.getRecipientUserId())
                .type(payload.getType())
                .title(payload.getTitle())
                .message(payload.getMessage())
                .resourceId(event.getResourceId())
                .workspaceId(event.getWorkspaceId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);

        NotificationResponse response = toResponse(saved);

        // 2. Cập nhật Redis cache
        cacheService.addToNotificationList(event.getRecipientUserId(), response);
        cacheService.incrementUnreadCount(event.getRecipientUserId());

        // 3. Gửi WebSocket realtime
        messagingTemplate.convertAndSendToUser(
                event.getRecipientUserId().toString(),
                "/queue/notifications",
                response
        );

        log.info("[Neural Bridge] Saved + Cached + Sent notification {} đến user {}",
                event.getEventType(), event.getRecipientUserId());
    }

    // Lấy danh sách khi user F5
    public List<NotificationResponse> getNotifications(UUID userId) {
        // 1. Thử lấy từ Redis
        List<NotificationResponse> cached = cacheService.getNotificationList(userId);
        if (cached != null) {
            log.info("[Redis] Cache hit - notifications for user {}", userId);
            return cached;
        }

        // 2. Cache miss → query DB
        log.info("[Redis] Cache miss - query DB for user {}", userId);
        List<NotificationResponse> fromDb = notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        // 3. Lưu vào Redis
        cacheService.setNotificationList(userId, fromDb);

        return fromDb;
    }

    // Đếm số chưa đọc
    public long countUnread(UUID userId) {
        // 1. Thử lấy từ Redis
        long cached = cacheService.getUnreadCount(userId);
        if (cached != -1L) {
            log.info("[Redis] Cache hit - unread count for user {}", userId);
            return cached;
        }

        // 2. Cache miss → query DB
        log.info("[Redis] Cache miss - query DB unread for user {}", userId);
        long count = notificationRepository
                .countByRecipientUserIdAndIsRead(userId, false);

        // 3. Lưu vào Redis
        cacheService.setUnreadCount(userId, count);

        return count;
    }

    // Đánh dấu tất cả đã đọc
    @Transactional
    public void markAllAsRead(UUID userId) {
        // 1. Cập nhật DB
        notificationRepository.markAllAsRead(userId);

        // 2. Reset Redis
        cacheService.resetUnreadCount(userId);
        cacheService.invalidateUser(userId); // xóa list cache để load lại
    }

    // Đánh dấu 1 cái đã đọc
    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            UUID userId = n.getRecipientUserId();

            // 1. Cập nhật DB
            n.setRead(true);
            notificationRepository.save(n);

            // 2. Cập nhật Redis
            cacheService.invalidateUser(userId); // xóa cache → load lại từ DB
        });
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .resourceId(n.getResourceId())
                .workspaceId(n.getWorkspaceId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private NotificationPayload buildPayload(NotificationEvent event) {
        // Override hoàn toàn nếu producer tự set title + message
        if (event.getCustomTitle() != null && event.getCustomMessage() != null) {
            return NotificationPayload.builder()
                    .type(event.getEventType())
                    .title(event.getCustomTitle())
                    .message(event.getCustomMessage())
                    .resourceId(event.getResourceId())
                    .workspaceId(event.getWorkspaceId())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        NotificationTemplate template = NotificationTemplate.fromEventType(event.getEventType());

        return NotificationPayload.builder()
                .type(event.getEventType())
                .title(template.getTitle())
                .message(template.buildMessage(event))
                .resourceId(event.getResourceId())
                .workspaceId(event.getWorkspaceId())
                .timestamp(LocalDateTime.now())
                .build();
    }
}