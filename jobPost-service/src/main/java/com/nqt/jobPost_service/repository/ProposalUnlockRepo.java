package com.nqt.jobPost_service.repository;

import com.nqt.jobPost_service.entity.ProposalUnlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProposalUnlockRepo extends JpaRepository<ProposalUnlock, UUID> {
    Optional<ProposalUnlock> findByProposalId(UUID proposalId);
}
