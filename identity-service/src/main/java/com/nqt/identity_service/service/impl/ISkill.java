package com.nqt.identity_service.service.impl;

import com.nqt.identity_service.dto.request.SkillRequest;
import com.nqt.identity_service.dto.response.SkillResponse;

import java.util.List;

public interface ISkill {
    SkillResponse createSkill(SkillRequest request);

    List<SkillResponse> getAllSkills();

    SkillResponse getSkillById(long id);

    SkillResponse updateSkill(long id, SkillRequest request);

    void deleteSkill(long id);

    List<SkillResponse> getAllSkillsByExpertCode(String expertCode);
}
