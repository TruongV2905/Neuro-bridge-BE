package com.nqt.notification_service.repository;

import com.nqt.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Lấy tất cả notification của user, mới nhất trước
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);

    // Đếm số chưa đọc
    long countByRecipientUserIdAndIsRead(UUID recipientUserId, boolean isRead);

    // Đánh dấu tất cả đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientUserId = :userId AND n.isRead = false")
    void markAllAsRead(UUID userId);
}
