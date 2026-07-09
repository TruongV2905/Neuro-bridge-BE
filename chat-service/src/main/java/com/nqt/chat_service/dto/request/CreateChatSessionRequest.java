package com.nqt.chat_service.dto.request;

import com.nqt.chat_service.constant.ChatType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateChatSessionRequest {
    UUID workspaceId;

    ChatType type;

    Set<String> participantCodes;
}
