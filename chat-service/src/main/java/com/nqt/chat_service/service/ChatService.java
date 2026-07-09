package com.nqt.chat_service.service;

import com.identity.grpc.UserResponse;
import com.nqt.chat_service.constant.ChatSessionStatus;
import com.nqt.chat_service.constant.ChatType;
import com.nqt.chat_service.constant.SenderRole;
import com.nqt.chat_service.dto.request.CreateChatSessionRequest;
import com.nqt.chat_service.dto.request.SendMessageRequest;
import com.nqt.chat_service.dto.response.ChatSessionResponse;
import com.nqt.chat_service.dto.response.MessageResponse;
import com.nqt.chat_service.dto.response.ParticipantDetail;
import com.nqt.chat_service.entity.ChatSession;
import com.nqt.chat_service.entity.Messages;
import com.nqt.chat_service.httpclient.IdentityServiceClient;
import com.nqt.chat_service.mapper.ChatSessionMapper;
import com.nqt.chat_service.mapper.MessageMapper;
import com.nqt.chat_service.repository.ChatSessionRepository;
import com.nqt.chat_service.repository.MessageRepository;
import com.nqt.common_starter.dto.response.PageResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ChatService {

    ChatSessionRepository chatSessionRepository;
    MessageRepository messageRepository;
    SimpMessagingTemplate messagingTemplate;
    ChatSessionMapper chatSessionMapper;
    MessageMapper messageMapper;
    IdentityServiceClient identityServiceClient;

    // ---------------------------------------------------------------------
    // Chat sessions
    // ---------------------------------------------------------------------

    @Transactional
    public ChatSessionResponse createChatSession(CreateChatSessionRequest request) {
        validateCreateChatSessionRequest(request);

        ChatSession chatSession = chatSessionMapper.toEntity(request);
        chatSession.setStatus(ChatSessionStatus.ACTIVE);
        if (chatSession.getType() == null) {
            chatSession.setType(ChatType.DIRECT);
        }

        Set<String> participants = new HashSet<>(request.getParticipantCodes());
        try {
            String me = identityServiceClient.getCurrentUser().getCode();
            if (me != null && !me.isBlank()) {
                participants.add(me);
            }
        } catch (Exception e) {
            log.warn("[Chat] Could not resolve current user when creating session: {}", e.getMessage());
        }
        chatSession.setParticipantCodes(participants);

        ChatSession saved = chatSessionRepository.save(chatSession);
        log.info("[Chat] Created session id={} workspace={} participants={}",
                saved.getId(), saved.getWorkspaceId(), saved.getParticipantCodes());

        return toSessionResponse(saved);
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse getChatSession(UUID chatSessionId) {
        return toSessionResponse(getSessionAsParticipant(chatSessionId));
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getMyChatSessions() {
        String userCode = identityServiceClient.getCurrentUser().getCode();
        if (userCode == null || userCode.isBlank()) {
            throw new RuntimeException("Cannot resolve current user");
        }

        List<ChatSession> sessions =
                chatSessionRepository.findByParticipantCodesContaining(userCode);

        sessions.sort(Comparator.comparing(ChatSession::getId,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return sessions.stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Transactional
    public ChatSessionResponse addParticipant(UUID chatSessionId, String userCode) {
        if (userCode == null || userCode.isBlank()) {
            throw new RuntimeException("User code is required");
        }
        ChatSession session = getSessionAsParticipant(chatSessionId);
        session.getParticipantCodes().add(userCode);
        ChatSession saved = chatSessionRepository.save(session);
        log.info("[Chat] Added participant={} to session={}", userCode, chatSessionId);
        return toSessionResponse(saved);
    }

    @Transactional
    public ChatSessionResponse removeParticipant(UUID chatSessionId, String userCode) {
        if (userCode == null || userCode.isBlank()) {
            throw new RuntimeException("User code is required");
        }
        ChatSession session = getSessionAsParticipant(chatSessionId);
        session.getParticipantCodes().remove(userCode);
        ChatSession saved = chatSessionRepository.save(session);
        log.info("[Chat] Removed participant={} from session={}", userCode, chatSessionId);
        return toSessionResponse(saved);
    }

    @Transactional
    public ChatSessionResponse closeChatSession(UUID chatSessionId) {
        ChatSession session = getSessionAsParticipant(chatSessionId);
        session.setStatus(ChatSessionStatus.CLOSED);
        ChatSession saved = chatSessionRepository.save(session);
        log.info("[Chat] Closed session={}", chatSessionId);
        return toSessionResponse(saved);
    }

    @Transactional
    public ChatSessionResponse blockChatSession(UUID chatSessionId) {
        ChatSession session = getSessionAsParticipant(chatSessionId);
        session.setStatus(ChatSessionStatus.BLOCKED);
        ChatSession saved = chatSessionRepository.save(session);
        log.info("[Chat] Blocked session={}", chatSessionId);
        return toSessionResponse(saved);
    }

    // ---------------------------------------------------------------------
    // Messages
    // ---------------------------------------------------------------------

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        validateSendMessageRequest(request);

        ChatSession chatSession = getSessionAsParticipant(request.getChatSessionId());
        if (chatSession.getStatus() != ChatSessionStatus.ACTIVE) {
            throw new RuntimeException("Chat session is not active");
        }

        UserResponse me = identityServiceClient.getCurrentUser();
        String myCode = me.getCode();
        SenderRole myRole = mapRoleToSenderRole(me.getRole());

        Messages messages = messageMapper.toEntity(request);
        messages.setChatSession(chatSession);
        messages.setSenderId(parseUuidOrNull(myCode));
        messages.setSenderRole(myRole);

        Messages saved = messageRepository.save(messages);

        MessageResponse response = toMessageResponse(saved, me);
        chatSession.getParticipantCodes().forEach(participantCode ->
                messagingTemplate.convertAndSendToUser(participantCode, "/queue/messages", response)
        );

        log.info("[Chat] Sent message id={} session={} from={} role={}",
                saved.getId(), chatSession.getId(), myCode, myRole);
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getMessages(UUID chatSessionId, int page, int size) {
        if (chatSessionId == null) {
            throw new RuntimeException("Chat session id is required");
        }
        ChatSession session = getSessionAsParticipant(chatSessionId);

        Page<Messages> msgs = messageRepository.findByChatSessionIdOrderByCreatedAtDesc(
                session.getId(), PageRequest.of(page, size)
        );

        return PageResponse.fromPage(msgs.map(m -> {
            MessageResponse response = messageMapper.toResponse(m);
            UUID senderId = m.getSenderId();
            String senderCode = null;
            if (senderId != null) {
                try {
                    senderCode = identityServiceClient.getUserByCode(senderId.toString()).getCode();
                } catch (Exception e) {
                    log.warn("[Chat] Cannot resolve sender {} for message {}: {}",
                            senderId, m.getId(), e.getMessage());
                }
            }
            response.setSenderCode(senderCode);
            return response;
        }));
    }

    // ---------------------------------------------------------------------
    // Response builders — enrich với participant details
    // ---------------------------------------------------------------------

    private ChatSessionResponse toSessionResponse(ChatSession session) {
        ChatSessionResponse response = chatSessionMapper.toResponse(session);
        response.setParticipants(resolveParticipants(session.getParticipantCodes()));
        return response;
    }

    private MessageResponse toMessageResponse(Messages m, UserResponse sender) {
        MessageResponse response = messageMapper.toResponse(m);
        response.setSenderCode(sender.getCode());
        return response;
    }

    /**
     * Gọi gRPC {@code getUserByCode} cho mỗi user code rồi map sang {@link ParticipantDetail}.
     * Fail 1 cái không chặn cả list — log warn và bỏ qua.
     */
    private List<ParticipantDetail> resolveParticipants(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        List<ParticipantDetail> details = new ArrayList<>(codes.size());
        for (String code : codes) {
            if (code == null || code.isBlank()) continue;
            try {
                UserResponse u = identityServiceClient.getUserByCode(code);
                details.add(ParticipantDetail.builder()
                        .code(u.getCode())
                        .fullName(u.getFullName())
                        .avatarUrl(u.getAvatarUrl())
                        .role(u.getRole())
                        .build());
            } catch (Exception e) {
                log.warn("[Chat] Cannot resolve participant code={}: {}", code, e.getMessage());
            }
        }
        // Trả về order ổn định theo code
        details.sort(Comparator.comparing(ParticipantDetail::getCode,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return details;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private ChatSession getSessionAsParticipant(UUID chatSessionId) {
        if (chatSessionId == null) {
            throw new RuntimeException("Chat session id is required");
        }
        ChatSession session = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));

        String myCode;
        try {
            myCode = identityServiceClient.getCurrentUser().getCode();
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve current user: " + e.getMessage());
        }

        if (myCode == null || !session.getParticipantCodes().contains(myCode)) {
            throw new RuntimeException("Current user is not participant of this chat session");
        }
        return session;
    }

    private SenderRole mapRoleToSenderRole(String role) {
        if (role == null) {
            return SenderRole.SYSTEM;
        }
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "CLIENT" -> SenderRole.CLIENT;
            case "EXPERT" -> SenderRole.EXPERT;
            default -> SenderRole.SYSTEM;
        };
    }

    private UUID parseUuidOrNull(String code) {
        if (code == null || code.isBlank()) return null;
        try {
            return UUID.fromString(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------------

    private void validateCreateChatSessionRequest(CreateChatSessionRequest request) {
        if (request == null) {
            throw new RuntimeException("Request is required");
        }
        Set<String> participants = request.getParticipantCodes();
        if (participants == null || participants.size() < 2) {
            throw new RuntimeException("Chat session must have at least 2 participants");
        }
        for (String p : participants) {
            if (p == null || p.isBlank()) {
                throw new RuntimeException("Participant code must not be blank");
            }
        }
    }

    private void validateSendMessageRequest(SendMessageRequest request) {
        if (request == null) {
            throw new RuntimeException("Request is required");
        }
        if (request.getChatSessionId() == null) {
            throw new RuntimeException("Chat session id is required");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new RuntimeException("Message content required");
        }
    }
}
