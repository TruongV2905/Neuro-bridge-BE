package com.nqt.rating_service.service;

import com.nqt.rating_service.dto.response.RatingSummaryResponse;
import com.nqt.rating_service.entity.RatingReview;
import com.nqt.rating_service.entity.RatingSummary;
import com.nqt.rating_service.enums.ReviewStatus;
import com.nqt.rating_service.enums.TargetType;
import com.nqt.rating_service.mapper.RatingMapper;
import com.nqt.rating_service.repository.RatingReviewRepository;
import com.nqt.rating_service.repository.RatingSummaryRepository;
import com.nqt.rating_service.service.impl.IRatingSummaryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RatingSummaryService implements IRatingSummaryService {
    RatingReviewRepository ratingReviewRepository;
    RatingSummaryRepository ratingSummaryRepository;
    RatingMapper ratingMapper;

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getExpertSummary(UUID expertId) {
        RatingSummary summary = ratingSummaryRepository
                .findByExpertIdAndTargetType(expertId, TargetType.EXPERT)
                .orElseGet(() -> RatingSummary.builder()
                        .expertId(expertId)
                        .targetType(TargetType.EXPERT)
                        .averageScore(BigDecimal.ZERO)
                        .totalReviews(0)
                        .totalCompletedOrders(0)
                        .lastUpdatedAt(LocalDateTime.now())
                        .build());

        return ratingMapper.toSummaryResponse(summary);
    }

    @Override
    @Transactional
    public RatingSummaryResponse recalculateExpertSummary(UUID expertId) {
        List<RatingReview> visibleReviews = ratingReviewRepository
                .findAllByExpertIdAndTargetTypeAndStatus(
                        expertId,
                        TargetType.EXPERT,
                        ReviewStatus.VISIBLE);

        int totalReviews = visibleReviews.size();
        BigDecimal averageScore = totalReviews == 0
                ? BigDecimal.ZERO
                : visibleReviews.stream()
                        .map(RatingReview::getRatingScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(totalReviews), 4, RoundingMode.HALF_UP);

        RatingSummary summary = ratingSummaryRepository
                .findByExpertIdAndTargetType(expertId, TargetType.EXPERT)
                .orElseGet(() -> RatingSummary.builder()
                        .expertId(expertId)
                        .targetType(TargetType.EXPERT)
                        .totalCompletedOrders(0)
                        .build());

        summary.setAverageScore(averageScore);
        summary.setTotalReviews(totalReviews);
        // TODO: Populate totalCompletedOrders from workspace-service when that integration is ready.
        summary.setLastUpdatedAt(LocalDateTime.now());

        return ratingMapper.toSummaryResponse(ratingSummaryRepository.save(summary));
    }
}
