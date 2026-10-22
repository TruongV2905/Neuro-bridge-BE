package com.nqt.chat_service.repository;

import com.nqt.chat_service.constant.ChatType;
import com.nqt.chat_service.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    Optional<ChatSession> findByWorkspaceIdAndType(UUID workspaceId, ChatType type);

    /**
     * Tìm các phòng chat mà userCode là participant.
     * {@code participantCodes} là @ElementCollection trên entity.
     */
    List<ChatSession> findByParticipantCodesContainingAndType(String userCode, ChatType type);
}
