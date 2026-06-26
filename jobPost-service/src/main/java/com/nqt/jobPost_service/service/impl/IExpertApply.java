package com.nqt.jobPost_service.service.impl;

import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.jobPost_service.dto.request.ExpertApplyRequest;
import com.nqt.jobPost_service.dto.response.ExpertApplyResponse;

import java.util.UUID;

public interface IExpertApply {

    ExpertApplyResponse create(ExpertApplyRequest request);

    ExpertApplyResponse update(UUID id, ExpertApplyRequest request);

    ExpertApplyResponse getById(UUID id);

    PageResponse<ExpertApplyResponse> getAll(PageRequest pageRequest);

    void delete(UUID id);
}
