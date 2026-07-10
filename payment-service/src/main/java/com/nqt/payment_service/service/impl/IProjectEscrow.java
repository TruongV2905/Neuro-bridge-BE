package com.nqt.payment_service.service.impl;

import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.payment_service.dto.request.ProjectEscrowRequest;
import com.nqt.payment_service.dto.response.ProjectEscrowResponse;
import com.nqt.payment_service.entity.ProjectEscrow;

import java.util.UUID;

public interface IProjectEscrow {

    ProjectEscrow create(ProjectEscrowRequest projectEscrowRequest);

    ProjectEscrowResponse getById(UUID id);
    ProjectEscrow findOrCreateByJobPost(UUID jobPostId, String clientCode, String expertCode);
    ProjectEscrow getEntity(UUID id);
    void markFunded(UUID escrowId);
}
