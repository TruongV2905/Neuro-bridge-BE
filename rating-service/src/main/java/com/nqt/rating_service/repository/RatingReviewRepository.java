package com.nqt.rating_service.repository;

import com.nqt.rating_service.entity.RatingReview;
import com.nqt.rating_service.enums.ReferenceType;
import com.nqt.rating_service.enums.ReviewStatus;
import com.nqt.rating_service.enums.TargetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RatingReviewRepository
        extends JpaRepository<RatingReview, UUID>, JpaSpecificationExecutor<RatingReview> {
    boolean existsByReviewerIdAndReferenceTypeAndReferenceId(
            UUID reviewerId, ReferenceType referenceType, UUID referenceId);

    Optional<RatingReview> findByReviewerIdAndReferenceTypeAndReferenceId(
            UUID reviewerId, ReferenceType referenceType, UUID referenceId);

    Page<RatingReview> findByExpertIdAndTargetTypeAndStatus(
            UUID expertId, TargetType targetType, ReviewStatus status, Pageable pageable);

    List<RatingReview> findAllByExpertIdAndTargetTypeAndStatus(
            UUID expertId, TargetType targetType, ReviewStatus status);
}
