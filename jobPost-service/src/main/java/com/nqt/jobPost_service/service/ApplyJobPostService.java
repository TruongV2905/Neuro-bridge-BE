package com.nqt.jobPost_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.jobPost_service.dto.request.ApplyJobPostRequest;
import com.nqt.jobPost_service.dto.response.ApplyJobPostResponse;
import com.nqt.jobPost_service.dto.response.ExpertApplyResponse;
import com.nqt.jobPost_service.dto.response.ProposalResponse;
import com.nqt.jobPost_service.dto.response.ProposalUnlockResponse;
import com.nqt.jobPost_service.entity.ExpertApply;
import com.nqt.jobPost_service.entity.JobPost;
import com.nqt.jobPost_service.entity.Proposal;
import com.nqt.jobPost_service.entity.ProposalUnlock;
import com.nqt.jobPost_service.mapper.ExpertApplyMapper;
import com.nqt.jobPost_service.mapper.ProposalMapper;
import com.nqt.jobPost_service.mapper.ProposalUnlockMapper;
import com.nqt.jobPost_service.repository.JobPostRepo;
import com.nqt.jobPost_service.service.impl.IApplyJobPost;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplyJobPostService implements IApplyJobPost {
    ExpertApplyService expertApplyService;

    ProposalService proposalService;
    ProposalUnlockService proposalUnlockService;
    JobPostRepo  jobPostRepo;
    ExpertApplyMapper expertApplyMapper;
    ProposalMapper proposalMapper;

    @Override
    public void appyJobPost(ApplyJobPostRequest request) {

        // 1. Create Expert Apply
        ExpertApplyResponse expertApplyResponse =
                expertApplyService.create(request.getExpertApplyRequest());

        // 2. Create Proposal
        ProposalResponse proposalResponse =
                proposalService.createForExpertApply(
                        expertApplyResponse.getId(),
                        request.getProposalRequest()
                );

        // 3. Create Proposal Unlock nếu có
        if (request.getProposalUnlockRequest() != null) {
            proposalUnlockService.createFromProposal(
                    proposalResponse.getId(),
                    request.getProposalUnlockRequest()
            );
        }
    }

    @Override
    public List<ApplyJobPostResponse> getAllApplyJobPostByIdJobPost(UUID jobPostId) {
        JobPost jobPost = jobPostRepo.findById(jobPostId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Job Post " + ErrorCode.NOT_FOUND.getMessage()
                ));

        Set<ExpertApply> expertApplies = jobPost.getExpertApplies();

        return expertApplies.stream()
                .map(this::toApplyJobPostResponse)
                .toList();
    }

    private ApplyJobPostResponse toApplyJobPostResponse(ExpertApply expertApply) {
        ExpertApplyResponse expertApplyResponse =
                expertApplyMapper.toResponse(expertApply);

        Proposal proposal = expertApply.getProposal();

        ProposalResponse proposalResponse = null;
        ProposalUnlockResponse proposalUnlockResponse = null;

        if (proposal != null) {
            proposalResponse = proposalMapper.toResponse(proposal);
            ProposalUnlock proposalUnlock = proposal.getProposalUnlock();
            if (proposalUnlock != null) {
                proposalResponse.setUnlockCost(proposalUnlock.getUnlockCost());
                proposalUnlockResponse =
                        proposalUnlockService.getById(proposalUnlock.getId());
            }
        }

        return ApplyJobPostResponse.builder()
                .expertApply(expertApplyResponse)
                .proposal(proposalResponse)
                .proposalUnlock(proposalUnlockResponse)
                .build();
    }

}
