package com.nqt.identity_service.mapper;

import com.nqt.identity_service.dto.request.register.ExpertRegisterRequest;
import com.nqt.identity_service.entity.ExpertProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExpertProfileMapper {


    @Mapping(target = "user", ignore = true)
    @Mapping(target = "skills", ignore = true)
    ExpertProfile toEntity(ExpertRegisterRequest request);
}
