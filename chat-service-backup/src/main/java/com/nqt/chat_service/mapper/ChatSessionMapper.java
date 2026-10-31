package com.nqt.chat_service.mapper;

import com.nqt.chat_service.dto.request.CreateChatSessionRequest;
import com.nqt.chat_service.dto.response.ChatSessionResponse;
import com.nqt.chat_service.entity.ChatSession;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatSessionMapper {


    ChatSession toEntity(CreateChatSessionRequest request);

    ChatSessionResponse toResponse(ChatSession entity);
}
