package com.nqt.identity_service.mapper;

import com.nqt.identity_service.dto.request.register.BaseRegisterRequest;
import com.nqt.identity_service.dto.request.register.ClientRegisterRequest;
import com.nqt.identity_service.dto.request.register.ExpertRegisterRequest;
import com.nqt.identity_service.dto.request.user.ClientRequest;
import com.nqt.identity_service.dto.request.user.ExpertRequest;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.entity.Skill;
import com.nqt.identity_service.entity.User;
import java.util.Collections;

import org.mapstruct.*;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {


    @Mapping(target = "password", ignore = true)
    @Mapping(target = "clientProfile", ignore = true)
    User toEntity(ClientRegisterRequest request);


    @Mapping(target = "password", ignore = true)
    @Mapping(target = "expertProfile", ignore = true)
    User toEntity(ExpertRegisterRequest request);

    @Mapping(target = "accessToken", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "companyName", source = "clientProfile.companyName")
    @Mapping(target = "industry", source = "clientProfile.industry")
    @Mapping(target = "companyDescription", source = "clientProfile.companyDescription")
    @Mapping(target = "skillIds", source = "expertProfile.skills")

    UserResponse toResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "clientProfile.companyName", source = "companyName")
    @Mapping(target = "clientProfile.industry", source = "industry")
    @Mapping(target = "clientProfile.companyDescription", source = "companyDescription")
    void updateUserFromClientRequest(ClientRequest request, @MappingTarget User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "expertProfile", ignore = true)

    void updateUserFromExpertRequest(ExpertRequest request, @MappingTarget User user);


    default Set<Long> mapSkillsToIds(Set<Skill> skills) {
        if (skills == null) {
            return Collections.emptySet();
        }
        return skills.stream()
                .map(com.nqt.identity_service.entity.Skill::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

}
