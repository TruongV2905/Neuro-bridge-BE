package com.nqt.payment_service.repository;

import com.nqt.payment_service.entity.ProjectEscrow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectEscrowRepo extends JpaRepository<ProjectEscrow, UUID> {
    Optional<ProjectEscrow> findByJobPostId(UUID jobPostId);
}
