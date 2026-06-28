package com.nqt.rating_service.repository;

import com.nqt.rating_service.entity.RatingSummary;
import com.nqt.rating_service.enums.TargetType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingSummaryRepository extends JpaRepository<RatingSummary, UUID> {
    Optional<RatingSummary> findByExpertIdAndTargetType(UUID expertId, TargetType targetType);
}
