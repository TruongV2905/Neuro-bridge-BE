package com.nqt.workspace_service.grpc;

import com.jobpost.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class JobPostServiceClient {

    @GrpcClient("jobpost-service")
    private ProposalUnlockGrpcServiceGrpc.ProposalUnlockGrpcServiceBlockingStub proposalUnlockStub;

    public ProposalUnlockResponse getApprovedProposalUnlock(UUID jobId) {
        JobIdRequest request = JobIdRequest.newBuilder()
                .setJobId(jobId.toString())
                .build();

        return proposalUnlockStub.getApprovedProposalUnlock(request);
    }

    public ProposalUnlockResponse updateProposalUnlock(UUID proposalUnlockId, String strategyDetail) {
        ProposalUnlockUpdateRequest request = ProposalUnlockUpdateRequest.newBuilder()
                .setProposalUnlockId(proposalUnlockId.toString())
                .setStrategyDetail(strategyDetail)
                .build();

        return proposalUnlockStub.updateProposalUnlock(request);
    }

    public ProposalUnlockResponse createProposalUnlockForJob(UUID jobId, String strategyDetail) {
        CreateProposalUnlockRequest request = CreateProposalUnlockRequest.newBuilder()
                .setJobId(jobId.toString())
                .setStrategyDetail(strategyDetail)
                .build();

        return proposalUnlockStub.createProposalUnlockForJob(request);
    }
}
