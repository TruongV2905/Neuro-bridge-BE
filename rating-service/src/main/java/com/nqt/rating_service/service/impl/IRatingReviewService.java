package com.nqt.rating_service.service.impl;

import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.rating_service.dto.request.CreateWorkspaceReviewRequest;
import com.nqt.rating_service.dto.request.RatingReviewFilterRequest;
import com.nqt.rating_service.dto.response.RatingReviewResponse;
import java.util.UUID;

public interface IRatingReviewService {
    RatingReviewResponse createWorkspaceReview(UUID workspaceId, CreateWorkspaceReviewRequest request);

    RatingReviewResponse getMyWorkspaceReview(UUID workspaceId);

    PageResponse<RatingReviewResponse> getVisibleReviewsByExpert(UUID expertId, PageRequest pageRequest);

    PageResponse<RatingReviewResponse> getReviewsForAdmin(RatingReviewFilterRequest request);

    void softDeleteReview(UUID reviewId);
}
