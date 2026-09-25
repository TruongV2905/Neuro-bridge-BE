package com.nqt.identity_service.mapper;

import com.nqt.identity_service.dto.request.register.ClientRegisterRequest;
import com.nqt.identity_service.dto.request.register.ExpertRegisterRequest;
import com.nqt.identity_service.dto.request.user.ClientRequest;
import com.nqt.identity_service.dto.request.user.ExpertRequest;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.entity.Skill;
import com.nqt.identity_service.entity.User;
import org.mapstruct.*;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "clientProfile", ignore = true)
    User toEntity(ClientRegisterRequest request);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "expertProfile", ignore = true)
    User toEntity(ExpertRegisterRequest request);

    @Mapping(target = "accessToken", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
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
                .map(Skill::getId)
                .collect(Collectors.toSet());
    }
}

