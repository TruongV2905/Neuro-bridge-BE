package com.nqt.jobPost_service.service.impl;

import com.nqt.jobPost_service.dto.request.ProposalUnlockRequest;
import com.nqt.jobPost_service.dto.response.ProposalUnlockResponse;

import java.util.UUID;

public interface IProposalUnlock {

    ProposalUnlockResponse createFromProposal(UUID proposalId, ProposalUnlockRequest request);

    ProposalUnlockResponse getById(UUID proposalId);
    ProposalUnlockResponse update(UUID proposalUnlockId, ProposalUnlockRequest request);
    ProposalUnlockResponse getByProposalId(UUID proposalId);
    void delete(UUID proposalUnlockId);
}
