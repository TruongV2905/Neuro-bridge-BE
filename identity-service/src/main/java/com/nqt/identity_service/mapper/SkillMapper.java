package com.nqt.identity_service.mapper;


import com.nqt.identity_service.dto.request.skill.SkillRequest;
import com.nqt.identity_service.dto.response.skill.SkillResponse;
import com.nqt.identity_service.entity.Skill;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SkillMapper {


    Skill toEntity(SkillRequest request);


    SkillResponse toResponse(Skill skill);

    void toUpdate(@MappingTarget Skill skill, SkillRequest request);
}
