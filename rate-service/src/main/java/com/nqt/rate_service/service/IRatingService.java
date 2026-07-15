package com.nqt.rate_service.service;

import com.nqt.rate_service.dto.request.RatingRequest;
import com.nqt.rate_service.dto.response.ExpertRatingSummaryResponse;
import com.nqt.rate_service.entity.Rating;

import java.util.UUID;

public interface IRatingService {

    Rating rateProject(UUID projectId, String currentUserId, RatingRequest request);

    ExpertRatingSummaryResponse getExpertRatingSummary(UUID expertId);

    Rating getByProjectId(UUID projectId);
}
