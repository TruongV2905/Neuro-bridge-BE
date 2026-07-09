package com.nqt.jobPost_service.repository;

import com.nqt.jobPost_service.constant.ApplyStatus;
import com.nqt.jobPost_service.entity.ExpertApply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpertApplyRepo extends JpaRepository<ExpertApply, UUID> {
    Page<ExpertApply> findByExpertCodeContainingIgnoreCase(
            String expertCode,
            Pageable pageable
    );

    Page<ExpertApply> findByExpertCode(
            String expertCode,
            Pageable pageable
    );
    List<ExpertApply> findAllByJobPost_Id(UUID jobPostId);
    Optional<ExpertApply> findByJobPost_IdAndStatus(UUID jobPostId, ApplyStatus status);

    boolean existsByJobPost_IdAndExpertCode(UUID jobPostId, String expertCode);

}
