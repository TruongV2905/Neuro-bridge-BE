package com.nqt.rate_service.repository;

import com.nqt.rate_service.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    boolean existsByProjectId(UUID projectId);

    Optional<Rating> findByProjectId(UUID projectId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.expertId = :expertId")
    Double findAverageScoreByExpertId(@Param("expertId") UUID expertId);

    long countByExpertId(UUID expertId);
}
