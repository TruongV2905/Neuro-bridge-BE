package com.nqt.jobPost_service.service.impl;

import com.nqt.jobPost_service.entity.Contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IContractService {
    Contract createContract(UUID jobId, UUID proposalUnlockId,
                            String clientId, String expertId,
                            String strategyDetail, BigDecimal unlockCost,
                            String projectName, LocalDateTime deadline);

    Contract sign(UUID contractId, String currentUserId);

    Contract getById(UUID contractId);

    Optional<Contract> findByProposalUnlockId(UUID proposalUnlockId);

    Contract markWorkspaceCreated(UUID contractId, UUID workspaceId);

    void markEscrowId(UUID contractId, UUID escrowId);
    Optional<Contract> findByJobId(UUID jobId);
}
