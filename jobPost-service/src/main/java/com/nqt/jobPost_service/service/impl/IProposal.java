package com.nqt.jobPost_service.service.impl;

import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.jobPost_service.dto.request.ProposalRequest;
import com.nqt.jobPost_service.dto.response.ProposalResponse;

import java.util.UUID;

public interface IProposal {

    ProposalResponse createForExpertApply(UUID expertApplyId, ProposalRequest request);

    ProposalResponse update(UUID proposalId, ProposalRequest request);

    ProposalResponse getById(UUID proposalId);

    void delete(UUID proposalId);

    PageResponse<ProposalResponse> getAllByExpertCode(String expertCode, PageRequest pageRequest);
}
