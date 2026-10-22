package com.nqt.chat_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.chat_service.constant.SenderRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {


    UUID id;

    UUID chatSessionId;

    String senderCode;

    SenderRole senderRole;

    String content;

    LocalDateTime createdAt;
}
