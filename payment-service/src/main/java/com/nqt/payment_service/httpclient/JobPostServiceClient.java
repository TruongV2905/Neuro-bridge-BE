package com.nqt.payment_service.httpclient;

import com.jobpost.grpc.ProposalIdRequest;
import com.jobpost.grpc.ProposalUnlockGrpcServiceGrpc;
import com.jobpost.grpc.ProposalUnlockInfoResponse;
import com.jobpost.grpc.ProposalUnlockStatusResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
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

    /**
     * Lấy thông tin unlock proposal (expertCode, amount) thông qua proposalId.
     * Dùng khi client mua proposal bằng ví — payment-service không cần biết
     * expertCode/amount trước, mà jobpost-service sẽ trả về qua gRPC.
     */
    public ProposalUnlockInfo getProposalUnlockInfo(String proposalId) {
        ProposalUnlockInfoResponse response = proposalUnlockStub.getProposalUnlockInfo(
                ProposalIdRequest.newBuilder()
                        .setProposalId(proposalId)
                        .build()
        );

        UUID parsedProposalId = UUID.fromString(response.getProposalId());
        String expertCode = response.getExpertCode();
        BigDecimal amount = new BigDecimal(response.getAmount());
        log.info("Retrieved ProposalUnlockInfo: proposalId={}, expertCode={}, amount={}",
                parsedProposalId, expertCode, amount);
        return new ProposalUnlockInfo(parsedProposalId, expertCode, amount);
    }

    public record ProposalUnlockInfo(UUID proposalId, String expertCode, BigDecimal amount) {
    }
}