package com.nqt.jobPost_service.grpc;

import com.jobpost.grpc.JobIdRequest;
import com.jobpost.grpc.ProposalUnlockGrpcServiceGrpc;
import com.jobpost.grpc.ProposalUnlockResponse;
import com.nqt.jobPost_service.constant.ApplyStatus;
import com.nqt.jobPost_service.dto.request.ProposalUnlockRequest;
import com.nqt.jobPost_service.entity.ExpertApply;
import com.nqt.jobPost_service.entity.Proposal;
import com.nqt.jobPost_service.entity.ProposalUnlock;
import com.nqt.jobPost_service.repository.ExpertApplyRepo;
import com.nqt.jobPost_service.repository.ProposalRepo;
import com.nqt.jobPost_service.service.ProposalUnlockService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class ProposalUnlockGrpcServer extends ProposalUnlockGrpcServiceGrpc.ProposalUnlockGrpcServiceImplBase {

    private final ProposalUnlockService proposalUnlockService;
    private final ExpertApplyRepo expertApplyRepo;
    private final ProposalRepo proposalRepo;

    @Override
    public void getApprovedProposalUnlock(JobIdRequest request,
                                          StreamObserver<ProposalUnlockResponse> responseObserver) {
        try {
            UUID jobId = UUID.fromString(request.getJobId());

            ExpertApply expertApply = expertApplyRepo
                    .findByJobPost_IdAndStatus(jobId, ApplyStatus.APPROVED)
                    .orElseThrow(() -> Status.NOT_FOUND
                            .withDescription("Approved ExpertApply not found for jobId: " + jobId)
                            .asRuntimeException());

            Proposal proposal = expertApply.getProposal();
            if (proposal == null || proposal.getProposalUnlock() == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Proposal or ProposalUnlock not found")
                        .asRuntimeException());
                return;
            }

            ProposalUnlock proposalUnlock = proposal.getProposalUnlock();

            // Đảm bảo mở khóa (phòng trường hợp client hire thẳng mà chưa unlock trước đó)
            if (proposal.isLocked()) {
                proposal.setLocked(false);
                proposalRepo.save(proposal);
            }

            ProposalUnlockResponse response = ProposalUnlockResponse.newBuilder()
                    .setProposalUnlockId(proposalUnlock.getId().toString())
                    .setStrategyDetail(proposalUnlock.getStrategyDetail())
                    .setUnlockCost(proposalUnlock.getUnlockCost().toPlainString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid jobId format")
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateProposalUnlock(com.jobpost.grpc.ProposalUnlockUpdateRequest request,
                                     StreamObserver<com.jobpost.grpc.ProposalUnlockResponse> responseObserver) {
        try {
            UUID proposalUnlockId = UUID.fromString(request.getProposalUnlockId());

            ProposalUnlockRequest updateRequest = ProposalUnlockRequest.builder()
                    .strategyDetail(request.getStrategyDetail())
                    .unlockCost(new BigDecimal(request.getUnlockCost()))
                    .build();

            com.nqt.jobPost_service.dto.response.ProposalUnlockResponse updated =
                    proposalUnlockService.update(proposalUnlockId, updateRequest);

            com.jobpost.grpc.ProposalUnlockResponse response = com.jobpost.grpc.ProposalUnlockResponse.newBuilder()
                    .setProposalUnlockId(updated.getId().toString())
                    .setStrategyDetail(updated.getStrategyDetail())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid input: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
