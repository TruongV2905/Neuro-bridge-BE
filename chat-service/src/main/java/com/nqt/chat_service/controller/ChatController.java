package com.nqt.chat_service.controller;

import com.nqt.chat_service.constant.ChatType;
import com.nqt.chat_service.dto.request.CreateChatSessionRequest;
import com.nqt.chat_service.dto.request.SendMessageRequest;
import com.nqt.chat_service.dto.response.ChatSessionResponse;
import com.nqt.chat_service.dto.response.MessageResponse;
import com.nqt.chat_service.service.ChatService;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/chat")
public class ChatController {

    ChatService chatService;

    // ---------------------------------------------------------------------
    // REST endpoints
    // ---------------------------------------------------------------------

    @PostMapping("/sessions")
    public ResponseEntity<APIResponse<ChatSessionResponse>> createSession(
            @Valid @RequestBody CreateChatSessionRequest request) {
        return ResponseEntity.ok(APIResponse.response(
                201,
                "Create chat session successfully",
                chatService.createChatSession(request)
        ));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<APIResponse<ChatSessionResponse>> getSessionById(@PathVariable UUID id) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Get chat session successfully",
                chatService.getChatSession(id)
        ));
    }

    /**
     * Lấy tất cả phòng chat của current user.
     */
    @GetMapping("/sessions/mine/direct")
    public ResponseEntity<APIResponse<List<ChatSessionResponse>>> getMySessions() {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Get my chat sessions successfully",
                chatService.getMyChatSessions(ChatType.DIRECT)
        ));
    }

    @GetMapping("/sessions/mine/project")
    public ResponseEntity<APIResponse<List<ChatSessionResponse>>> getMySessionsInProject() {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Get my chat sessions successfully",
                chatService.getMyChatSessions(ChatType.PROJECT)
        ));
    }



    @PostMapping("/sessions/{id}/participants/{userCode}")
    public ResponseEntity<APIResponse<ChatSessionResponse>> addParticipant(
            @PathVariable UUID id,
            @PathVariable String userCode) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Add participant successfully",
                chatService.addParticipant(id, userCode)
        ));
    }

    @DeleteMapping("/sessions/{id}/participants/{userCode}")
    public ResponseEntity<APIResponse<ChatSessionResponse>> removeParticipant(
            @PathVariable UUID id,
            @PathVariable String userCode) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Remove participant successfully",
                chatService.removeParticipant(id, userCode)
        ));
    }

    @PostMapping("/sessions/{id}/close")
    public ResponseEntity<APIResponse<ChatSessionResponse>> closeSession(@PathVariable UUID id) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Close chat session successfully",
                chatService.closeChatSession(id)
        ));
    }

    @PostMapping("/sessions/{id}/block")
    public ResponseEntity<APIResponse<ChatSessionResponse>> blockSession(@PathVariable UUID id) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Block chat session successfully",
                chatService.blockChatSession(id)
        ));
    }

    /**
     * Lấy message theo phòng chat, có phân trang.
     */
    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<APIResponse<PageResponse<MessageResponse>>> getMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Get messages successfully",
                chatService.getMessages(id, page, size)
        ));
    }

    /**
     * REST alias để gửi message (ngoài WebSocket).
     * WebSocket vẫn dùng sendMessage(...) qua /app/chat.send bên dưới.
     */
    @PostMapping("/messages")
    public ResponseEntity<APIResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(APIResponse.response(
                201,
                "Send message successfully",
                chatService.sendMessage(request)
        ));
    }

    // ---------------------------------------------------------------------
    // STOMP message mapping — FE gửi tới /app/chat.send
    // ---------------------------------------------------------------------

    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public MessageResponse sendMessageStomp(SendMessageRequest request) {
        return chatService.sendMessage(request);
    }
}
