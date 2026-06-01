package com.nqt.identity_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.identity_service.dto.request.SkillRequest;
import com.nqt.identity_service.dto.response.SkillResponse;
import com.nqt.identity_service.entity.Skill;
import com.nqt.identity_service.mapper.SkillMapper;
import com.nqt.identity_service.repository.SkillRepository;
import com.nqt.identity_service.service.impl.ISkill;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SkillService implements ISkill {
    SkillRepository skillRepository;
    SkillMapper skillMapper;


    @Override
    public SkillResponse createSkill(SkillRequest request) {
        Skill skill = skillMapper.toEntity(request);

        return skillMapper.toResponse(skillRepository.save(skill));
    }

    @Override
    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAll()
                .stream()
                .map(skillMapper::toResponse)
                .toList();
    }

    @Override
    public SkillResponse getSkillById(long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND.getCode(),ErrorCode.NOT_FOUND.getMessage()));
        return skillMapper.toResponse(skill);
    }

    @Override
    public SkillResponse updateSkill(long id, SkillRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND.getCode(),ErrorCode.NOT_FOUND.getMessage()));

        skillMapper.toUpdate(skill, request);

        return skillMapper.toResponse(skillRepository.save(skill));
    }

    @Override
    public void deleteSkill(long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND.getCode(),ErrorCode.NOT_FOUND.getMessage()));

        skill.setDeleted(true);
        skillRepository.save(skill);
    }
}
