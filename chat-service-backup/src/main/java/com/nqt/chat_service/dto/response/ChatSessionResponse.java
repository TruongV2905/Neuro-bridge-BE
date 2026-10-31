package com.nqt.chat_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.chat_service.constant.ChatSessionStatus;
import com.nqt.chat_service.constant.ChatType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatSessionResponse {
    UUID id;

    UUID workspaceId;

    ChatSessionStatus status;

    ChatType type;

    LocalDateTime createdAt;

    /** Giữ lại để tương thích ngược — chỉ là mã user. */
    Set<String> participantCodes;

    /** Danh sách chi tiết participant để FE render avatar + tên + role. */
    List<ParticipantDetail> participants;
}
