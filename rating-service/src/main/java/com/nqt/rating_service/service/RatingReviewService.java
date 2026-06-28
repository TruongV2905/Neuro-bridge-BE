package com.nqt.rating_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.common_starter.util.PageUtil;
import com.nqt.rating_service.dto.request.CreateWorkspaceReviewRequest;
import com.nqt.rating_service.dto.request.RatingReviewFilterRequest;
import com.nqt.rating_service.dto.response.RatingReviewResponse;
import com.nqt.rating_service.entity.RatingReview;
import com.nqt.rating_service.enums.ReferenceType;
import com.nqt.rating_service.enums.ReviewStatus;
import com.nqt.rating_service.enums.TargetType;
import com.nqt.rating_service.mapper.RatingMapper;
import com.nqt.rating_service.repository.RatingReviewRepository;
import com.nqt.rating_service.service.impl.IRatingReviewService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RatingReviewService implements IRatingReviewService {
    RatingReviewRepository ratingReviewRepository;
    RatingSummaryService ratingSummaryService;
    RatingMapper ratingMapper;

    @Override
    @Transactional
    public RatingReviewResponse createWorkspaceReview(
            UUID workspaceId,
            CreateWorkspaceReviewRequest request
    ) {
        UUID reviewerId = getCurrentUserId();

        if (ratingReviewRepository.existsByReviewerIdAndReferenceTypeAndReferenceId(
                reviewerId,
                ReferenceType.WORKSPACE,
                workspaceId)) {
            throw new GlobalException(
                    ErrorCode.IS_EXISTED.getCode(),
                    "Client already reviewed this workspace");
        }

        // TODO: Replace request expertId with WorkspaceRatingClient-derived expertId.
        RatingReview review = RatingReview.builder()
                .reviewerId(reviewerId)
                .expertId(request.getExpertId())
                .referenceId(workspaceId)
                .referenceType(ReferenceType.WORKSPACE)
                .targetType(TargetType.EXPERT)
                .ratingScore(request.getRatingScore())
                .comment(request.getComment())
                .status(ReviewStatus.VISIBLE)
                .build();

        RatingReview savedReview = ratingReviewRepository.save(review);
        ratingSummaryService.recalculateExpertSummary(savedReview.getExpertId());

        return ratingMapper.toReviewResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingReviewResponse getMyWorkspaceReview(UUID workspaceId) {
        UUID reviewerId = getCurrentUserId();

        RatingReview review = ratingReviewRepository
                .findByReviewerIdAndReferenceTypeAndReferenceId(
                        reviewerId,
                        ReferenceType.WORKSPACE,
                        workspaceId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Rating review " + ErrorCode.NOT_FOUND.getMessage()));

        return ratingMapper.toReviewResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RatingReviewResponse> getVisibleReviewsByExpert(
            UUID expertId,
            PageRequest pageRequest
    ) {
        Pageable pageable = PageUtil.createPageable(pageRequest);
        Page<RatingReviewResponse> reviews = ratingReviewRepository
                .findByExpertIdAndTargetTypeAndStatus(
                        expertId,
                        TargetType.EXPERT,
                        ReviewStatus.VISIBLE,
                        pageable)
                .map(ratingMapper::toReviewResponse);

        return PageResponse.fromPage(reviews);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RatingReviewResponse> getReviewsForAdmin(RatingReviewFilterRequest request) {
        Pageable pageable = PageUtil.createPageable(request);
        Page<RatingReviewResponse> reviews = ratingReviewRepository
                .findAll(createAdminSpecification(request), pageable)
                .map(ratingMapper::toReviewResponse);

        return PageResponse.fromPage(reviews);
    }

    @Override
    @Transactional
    public void softDeleteReview(UUID reviewId) {
        UUID adminId = getCurrentUserId();

        RatingReview review = ratingReviewRepository.findById(reviewId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Rating review " + ErrorCode.NOT_FOUND.getMessage()));

        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new GlobalException(
                    ErrorCode.DATA_INVALID.getCode(),
                    "Rating review is already deleted");
        }

        review.setStatus(ReviewStatus.DELETED);
        review.setDeletedAt(LocalDateTime.now());
        review.setDeletedBy(adminId);

        RatingReview savedReview = ratingReviewRepository.save(review);
        ratingSummaryService.recalculateExpertSummary(savedReview.getExpertId());
    }

    private Specification<RatingReview> createAdminSpecification(RatingReviewFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getSearch() != null && !request.getSearch().isBlank()) {
                String keyword = "%" + request.getSearch().trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("comment")),
                        keyword));
            }

            if (request.getReviewerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("reviewerId"), request.getReviewerId()));
            }
            if (request.getExpertId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("expertId"), request.getExpertId()));
            }
            if (request.getReferenceId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("referenceId"), request.getReferenceId()));
            }
            if (request.getReferenceType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("referenceType"), request.getReferenceType()));
            }
            if (request.getTargetType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetType"), request.getTargetType()));
            }
            if (request.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new GlobalException(
                    ErrorCode.UNAUTHENTICATED.getCode(),
                    ErrorCode.UNAUTHENTICATED.getMessage());
        }

        return UUID.fromString(authentication.getName());
    }
}
