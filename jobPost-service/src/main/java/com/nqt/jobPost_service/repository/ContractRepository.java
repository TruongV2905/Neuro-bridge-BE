package com.nqt.jobPost_service.repository;

import com.nqt.jobPost_service.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {
    Optional<Contract> findByProposalUnlockId(UUID proposalUnlockId);
}
