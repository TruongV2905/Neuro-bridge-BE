package com.nqt.jobPost_service.service;

import com.identity.grpc.UserResponse;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.jobPost_service.dto.request.ProposalUnlockRequest;
import com.nqt.jobPost_service.dto.response.ProposalUnlockResponse;
import com.nqt.jobPost_service.entity.ExpertApply;
import com.nqt.jobPost_service.entity.JobPost;
import com.nqt.jobPost_service.entity.Proposal;
import com.nqt.jobPost_service.entity.ProposalUnlock;
import com.nqt.jobPost_service.mapper.ProposalUnlockMapper;
import com.nqt.jobPost_service.repository.ProposalRepo;
import com.nqt.jobPost_service.repository.ProposalUnlockRepo;
import com.nqt.jobPost_service.httpclient.IdentityServiceClient;
import com.nqt.jobPost_service.service.impl.IProposalUnlock;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProposalUnlockService implements IProposalUnlock {
    ProposalUnlockRepo proposalUnlockRepo;
    ProposalRepo proposalRepo;
    ProposalUnlockMapper proposalUnlockMapper;
    IdentityServiceClient identityServiceClient;

    @Override
    @Transactional
    public ProposalUnlockResponse createFromProposal(UUID proposalId, ProposalUnlockRequest request) {
        Proposal proposal = proposalRepo.findById(proposalId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Proposal " + ErrorCode.NOT_FOUND.getMessage()
                ));

        if (proposal.getProposalUnlock() != null) {
            throw new GlobalException(
                    ErrorCode.INVALID_CODE.getCode(),
                    "Proposal already unlocked"
            );
        }

        ProposalUnlock proposalUnlock = proposalUnlockMapper.toEntity(request);

        proposal.setProposalUnlock(proposalUnlock);
        proposal.setLocked(true);

        Proposal savedProposal = proposalRepo.save(proposal);

        return proposalUnlockMapper.toResponse(savedProposal.getProposalUnlock());
    }

    @Override
    public ProposalUnlockResponse getById(UUID proposalUnlockId) {
        UserResponse userResponse = identityServiceClient.getCurrentUser();

        ProposalUnlock proposalUnlock = proposalUnlockRepo.findById(proposalUnlockId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Proposal Unlock " + ErrorCode.NOT_FOUND.getMessage()
                ));

        if(!validateGetProposalUnlock(userResponse.getCode(),proposalUnlock)){
            return null;
        }

        return proposalUnlockMapper.toResponse(proposalUnlock);
    }

    @Override
    public ProposalUnlockResponse getByProposalId(UUID proposalId) {
        UserResponse userResponse = identityServiceClient.getCurrentUser();

        ProposalUnlock proposalUnlock = proposalUnlockRepo.findByProposalId(proposalId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Proposal Unlock " + ErrorCode.NOT_FOUND.getMessage()
                ));
        validateGetProposalUnlock(userResponse.getCode(), proposalUnlock);

        return proposalUnlockMapper.toResponse(proposalUnlock);
    }

    private boolean validateGetProposalUnlock(String code, ProposalUnlock proposalUnlock) {
        if (code == null || proposalUnlock == null) {
            return false;
        }

        Proposal proposal = proposalUnlock.getProposal();
        if (proposal == null) {
            return false;
        }

        ExpertApply expertApply = proposal.getExpertApply();
        if (expertApply == null) {
            return false;
        }

        JobPost jobPost = expertApply.getJobPost();
        if (jobPost == null) {
            return false;
        }

        boolean isProposalCreator = code.equals(expertApply.getExpertCode());
        if (isProposalCreator) {
            return true;
        }

        boolean isJobPostCreator = code.equals(jobPost.getCreatorCode());

        // Client chỉ được xem khi proposal đã unlock
        return isJobPostCreator && !proposal.isLocked();
    }

    @Override
    @Transactional
    public ProposalUnlockResponse update(UUID proposalUnlockId, ProposalUnlockRequest request) {
        UserResponse userResponse = identityServiceClient.getCurrentUser();

        ProposalUnlock proposalUnlock = proposalUnlockRepo.findById(proposalUnlockId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Proposal Unlock " + ErrorCode.NOT_FOUND.getMessage()
                ));

        validateGetProposalUnlock(userResponse.getCode(), proposalUnlock);

        proposalUnlockMapper.toUpdate(request, proposalUnlock);

        ProposalUnlock savedProposalUnlock = proposalUnlockRepo.save(proposalUnlock);

        return proposalUnlockMapper.toResponse(savedProposalUnlock);
    }


    @Override
    @Transactional
    public void delete(UUID proposalUnlockId) {
        ProposalUnlock proposalUnlock = proposalUnlockRepo.findById(proposalUnlockId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Proposal Unlock " + ErrorCode.NOT_FOUND.getMessage()
                ));

        Proposal proposal = proposalUnlock.getProposal();

        if (proposal != null) {
            proposal.setProposalUnlock(null);
            proposal.setLocked(false);

            proposalRepo.saveAndFlush(proposal);
        }

        proposalUnlockRepo.delete(proposalUnlock);
    }
}
