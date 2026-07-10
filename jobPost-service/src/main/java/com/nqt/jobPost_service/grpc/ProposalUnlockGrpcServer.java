package com.nqt.jobPost_service.grpc;

import com.jobpost.grpc.*;
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
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
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

    @Override
    public void unlockProposal(ProposalIdRequest request,
                               StreamObserver<ProposalUnlockStatusResponse> responseObserver) {
        try {
            UUID proposalId = UUID.fromString(request.getProposalId());

            Proposal proposal = proposalRepo.findById(proposalId)
                    .orElseThrow(() -> Status.NOT_FOUND
                            .withDescription("Proposal not found with id: " + proposalId)
                            .asRuntimeException());

            if (proposal.isLocked()) {
                proposal.setLocked(false);
                proposalRepo.save(proposal);
                log.info("Unlocked proposal id={}", proposalId);
            } else {
                log.info("Proposal id={} was already unlocked, no-op", proposalId);
            }

            ProposalUnlockStatusResponse response = ProposalUnlockStatusResponse.newBuilder()
                    .setProposalId(proposalId.toString())
                    .setUnlocked(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid proposalId format: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Failed to unlock proposal id={}", request.getProposalId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }


    @Override
    public void createProposalUnlockForJob(CreateProposalUnlockRequest request,
                                           StreamObserver<ProposalUnlockResponse> responseObserver) {
        try {
            UUID jobId = UUID.fromString(request.getJobId());

            ExpertApply expertApply = expertApplyRepo
                    .findByJobPost_IdAndStatus(jobId, ApplyStatus.APPROVED)
                    .orElseThrow(() -> Status.NOT_FOUND
                            .withDescription("Approved ExpertApply not found for jobId: " + jobId)
                            .asRuntimeException());

            Proposal proposal = expertApply.getProposal();

            if (proposal != null && proposal.getProposalUnlock() != null) {
                responseObserver.onError(Status.ALREADY_EXISTS
                        .withDescription("ProposalUnlock already exists for this job, use update instead")
                        .asRuntimeException());
                return;
            }

            if (proposal == null) {
                proposal = Proposal.builder()
                        .title(expertApply.getJobPost().getTitle())
                        .isLocked(false)
                        .build();
            }

            ProposalUnlock proposalUnlock = ProposalUnlock.builder()
                    .strategyDetail(request.getStrategyDetail())
                    .unlockCost(null)
                    .build();

            proposal.setProposalUnlock(proposalUnlock);
            expertApply.setProposal(proposal);

            ExpertApply savedExpertApply = expertApplyRepo.save(expertApply);
            ProposalUnlock savedProposalUnlock = savedExpertApply.getProposal().getProposalUnlock();

            ProposalUnlockResponse response = ProposalUnlockResponse.newBuilder()
                    .setProposalUnlockId(savedProposalUnlock.getId().toString())
                    .setStrategyDetail(savedProposalUnlock.getStrategyDetail())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid input: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Failed to create proposal unlock for jobId={}", request.getJobId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
