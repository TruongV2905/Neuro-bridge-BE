package com.nqt.jobPost_service.repository;

import com.nqt.jobPost_service.entity.ExpertApply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
