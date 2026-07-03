package com.nqt.jobPost_service.service.impl;

import com.nqt.jobPost_service.dto.response.HireResponse;

import java.util.UUID;

public interface IHireService {
    HireResponse hireExpert(UUID jobPostId, UUID expertApplyId);
}
