package com.nqt.jobPost_service.service;

import com.identity.grpc.UserResponse;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.jobPost_service.dto.request.ProposalUnlockRequest;
import com.nqt.jobPost_service.dto.response.ProposalUnlockResponse;
import com.nqt.jobPost_service.entity.ExpertApply;
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

        validateGetProposalUnlock(userResponse.getCode(),proposalUnlock);

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

    private void validateGetProposalUnlock(String code, ProposalUnlock proposalUnlock) {
        if (proposalUnlock.getProposal() == null) {
            throw new GlobalException(
                    ErrorCode.NOT_FOUND.getCode(),
                    "Proposal " + ErrorCode.NOT_FOUND.getMessage()
            );
        }

        Proposal proposal = proposalUnlock.getProposal();

        if (!proposal.isLocked()) {
            throw new GlobalException(
                    ErrorCode.UNAUTHORIZED.getCode(),
                    "Proposal is not locked"
            );
        }

        if (proposal.getExpertApply() == null) {
            throw new GlobalException(
                    ErrorCode.NOT_FOUND.getCode(),
                    "Expert Apply " + ErrorCode.NOT_FOUND.getMessage()
            );
        }

        ExpertApply expertApply = proposal.getExpertApply();

        if (expertApply.getJobPost() == null) {
            throw new GlobalException(
                    ErrorCode.NOT_FOUND.getCode(),
                    "Job Post " + ErrorCode.NOT_FOUND.getMessage()
            );
        }

        String creatorCode = expertApply.getJobPost().getCreatorCode();
        String expertCode = expertApply.getExpertCode();

        boolean isJobPostCreator = creatorCode.equals(code);
        boolean isProposalCreator = expertCode.equals(code);

        if (!isJobPostCreator && !isProposalCreator) {
            throw new GlobalException(
                    ErrorCode.UNAUTHORIZED.getCode(),
                    "You don't have permission to access this proposal unlock"
            );
        }
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
