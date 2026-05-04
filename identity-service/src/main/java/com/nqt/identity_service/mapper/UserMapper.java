package com.nqt.identity_service.mapper;

import com.nqt.identity_service.dto.request.RegisterRequest;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface UserMapper {

    User toEntity(RegisterRequest request);

    @Mapping(
            target = "roles",
            expression = "java(user.getRoles() == null ? java.util.Collections.emptyList() : " +
                    "user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toList()))"
    )
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    UserResponse toResponse(User user);

}
