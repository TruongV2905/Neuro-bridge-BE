package com.nqt.identity_service.mapper;

import com.nqt.identity_service.dto.request.RegisterRequest;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    User toEntity(RegisterRequest request);

    @Mapping(target = "accessToken", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    UserResponse toResponse(User user);

}
