package com.nqt.chat_service.dto.request;

import com.nqt.chat_service.constant.SenderRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendMessageRequest {
    UUID chatSessionId;
    String content;
}
