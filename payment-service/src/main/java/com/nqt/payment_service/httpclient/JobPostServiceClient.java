package com.nqt.payment_service.httpclient;

import com.jobpost.grpc.ProposalIdRequest;
import com.jobpost.grpc.ProposalUnlockGrpcServiceGrpc;
import com.jobpost.grpc.ProposalUnlockStatusResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class JobPostServiceClient {

    @GrpcClient("jobpost-service")
    private ProposalUnlockGrpcServiceGrpc.ProposalUnlockGrpcServiceBlockingStub proposalUnlockStub;

    public boolean unlockProposal(String proposalId) {
        ProposalUnlockStatusResponse response = proposalUnlockStub.unlockProposal(
                ProposalIdRequest.newBuilder()
                        .setProposalId(proposalId)
                        .build()
        );
        return response.getUnlocked();
    }
}