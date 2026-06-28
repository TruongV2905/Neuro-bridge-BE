package com.nqt.rating_service.mapper;

import com.nqt.rating_service.dto.response.RatingReviewResponse;
import com.nqt.rating_service.dto.response.RatingSummaryResponse;
import com.nqt.rating_service.entity.RatingReview;
import com.nqt.rating_service.entity.RatingSummary;
import org.springframework.stereotype.Component;

@Component
public class RatingMapper {
    public RatingReviewResponse toReviewResponse(RatingReview review) {
        if (review == null) {
            return null;
        }

        return RatingReviewResponse.builder()
                .id(review.getId())
                .reviewerId(review.getReviewerId())
                .expertId(review.getExpertId())
                .referenceId(review.getReferenceId())
                .referenceType(review.getReferenceType())
                .targetType(review.getTargetType())
                .ratingScore(review.getRatingScore())
                .comment(review.getComment())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .deletedAt(review.getDeletedAt())
                .deletedBy(review.getDeletedBy())
                .build();
    }

    public RatingSummaryResponse toSummaryResponse(RatingSummary summary) {
        if (summary == null) {
            return null;
        }

        return RatingSummaryResponse.builder()
                .id(summary.getId())
                .expertId(summary.getExpertId())
                .targetType(summary.getTargetType())
                .averageScore(summary.getAverageScore())
                .totalReviews(summary.getTotalReviews())
                .totalCompletedOrders(summary.getTotalCompletedOrders())
                .lastUpdatedAt(summary.getLastUpdatedAt())
                .build();
    }
}
