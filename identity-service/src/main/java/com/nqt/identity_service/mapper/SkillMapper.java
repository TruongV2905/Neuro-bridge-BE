package com.nqt.identity_service.mapper;


import com.nqt.identity_service.dto.request.SkillRequest;
import com.nqt.identity_service.dto.response.SkillResponse;
import com.nqt.identity_service.entity.Skill;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SkillMapper {


    Skill toEntity(SkillRequest request);


    SkillResponse toResponse(Skill skill);

    void toUpdate(@MappingTarget Skill skill, SkillRequest request);
}
