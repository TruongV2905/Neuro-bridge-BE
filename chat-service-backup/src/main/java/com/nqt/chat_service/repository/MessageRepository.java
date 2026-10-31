package com.nqt.chat_service.repository;

import com.nqt.chat_service.entity.Messages;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Messages, UUID> {

    /**
     * Lấy tin nhắn theo chatSessionId, sắp xếp mới nhất trước.
     */
    Page<Messages> findByChatSessionIdOrderByCreatedAtDesc(UUID chatSessionId, Pageable pageable);
}
