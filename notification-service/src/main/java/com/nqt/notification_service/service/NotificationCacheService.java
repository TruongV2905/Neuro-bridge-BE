package com.nqt.notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nqt.notification_service.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // Key pattern
    private static final String UNREAD_COUNT_KEY = "notification:unread:%s";   // userId
    private static final String NOTIFICATION_LIST_KEY = "notification:list:%s"; // userId
    private static final Duration TTL = Duration.ofHours(24); // cache 24h

    // ── Unread Count ─────────────────────────────────────────────

    public long getUnreadCount(UUID userId) {
        String key = String.format(UNREAD_COUNT_KEY, userId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value.toString()) : -1L;
        // -1 nghĩa là chưa có trong cache → cần query DB
    }

    public void setUnreadCount(UUID userId, long count) {
        String key = String.format(UNREAD_COUNT_KEY, userId);
        redisTemplate.opsForValue().set(key, count, TTL);
    }

    public void incrementUnreadCount(UUID userId) {
        String key = String.format(UNREAD_COUNT_KEY, userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, TTL);
        }
    }

    public void resetUnreadCount(UUID userId) {
        String key = String.format(UNREAD_COUNT_KEY, userId);
        redisTemplate.opsForValue().set(key, 0L, TTL);
    }

    // ── Notification List ─────────────────────────────────────────

    public List<NotificationResponse> getNotificationList(UUID userId) {
        String key = String.format(NOTIFICATION_LIST_KEY, userId);
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) return null; // null → cần query DB

        try {
            return objectMapper.convertValue(value,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, NotificationResponse.class));
        } catch (Exception e) {
            log.warn("[Redis] Failed to deserialize notification list for user {}", userId);
            return null;
        }
    }

    public void setNotificationList(UUID userId, List<NotificationResponse> list) {
        String key = String.format(NOTIFICATION_LIST_KEY, userId);
        redisTemplate.opsForValue().set(key, list, TTL);
    }

    public void addToNotificationList(UUID userId, NotificationResponse notification) {
        String key = String.format(NOTIFICATION_LIST_KEY, userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            List<NotificationResponse> current = getNotificationList(userId);
            if (current != null) {
                current.add(0, notification); // thêm vào đầu
                redisTemplate.opsForValue().set(key, current, TTL);
            }
        }
    }

    // ── Invalidate ────────────────────────────────────────────────

    public void invalidateUser(UUID userId) {
        redisTemplate.delete(String.format(UNREAD_COUNT_KEY, userId));
        redisTemplate.delete(String.format(NOTIFICATION_LIST_KEY, userId));
    }
}