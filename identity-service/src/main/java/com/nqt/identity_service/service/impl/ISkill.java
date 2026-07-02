package com.nqt.identity_service.service.impl;

import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.identity_service.dto.request.skill.SkillForExpertRequest;
import com.nqt.identity_service.dto.request.skill.SkillRequest;
import com.nqt.identity_service.dto.request.skill.SkillSuggestionRequest;
import com.nqt.identity_service.dto.response.skill.SkillResponse;
import com.nqt.identity_service.dto.response.skill.SkillSuggestionResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public interface ISkill {
    SkillResponse createSkill(SkillRequest request);

    void createSkillsByFile(MultipartFile file);

    PageResponse<SkillResponse> getAllSkills(PageRequest pageRequest);

    PageResponse<SkillResponse> getAllSkillsForAdmin(PageRequest pageRequest);

    SkillResponse getSkillById(long id);

    SkillResponse updateSkill(long id, SkillRequest request);

    void toggleSkill(long id);

    List<SkillResponse> getAllSkillsByExpertCode(String expertCode);

    SkillSuggestionResult suggestSkills(SkillSuggestionRequest request);

    void updateSkillForExpert(String expertCode, SkillForExpertRequest request);
}
