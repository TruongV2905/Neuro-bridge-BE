package com.nqt.workspace_service.grpc;

import com.jobpost.grpc.JobIdRequest;
import com.jobpost.grpc.ProposalUnlockGrpcServiceGrpc;
import com.jobpost.grpc.ProposalUnlockResponse;
import com.jobpost.grpc.ProposalUnlockUpdateRequest;
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

    public ProposalUnlockResponse updateProposalUnlock(UUID proposalUnlockId, String strategyDetail, BigDecimal unlockCost) {
        ProposalUnlockUpdateRequest request = ProposalUnlockUpdateRequest.newBuilder()
                .setProposalUnlockId(proposalUnlockId.toString())
                .setStrategyDetail(strategyDetail)
                .setUnlockCost(unlockCost.toPlainString())
                .build();

        return proposalUnlockStub.updateProposalUnlock(request);
    }
}
