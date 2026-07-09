package com.nqt.chat_service.mapper;

import com.nqt.chat_service.dto.request.SendMessageRequest;
import com.nqt.chat_service.dto.response.MessageResponse;
import com.nqt.chat_service.entity.Messages;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {


    Messages toEntity(SendMessageRequest request);

    @Mapping(target = "chatSessionId", source = "chatSession.id")
    MessageResponse toResponse(Messages entity);
}
