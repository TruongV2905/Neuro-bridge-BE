package com.nqt.workspace_service.service.interfaces;

import com.nqt.workspace_service.dto.response.ProposalStrategyResponse;

import java.util.UUID;

public interface IProposalStrategyService {
    ProposalStrategyResponse getProposalStrategy(UUID workspaceId, UUID currentUserId);
    ProposalStrategyResponse updateProposalStrategy(UUID workspaceId, UUID currentUserId, String newStrategyDetail);
}
