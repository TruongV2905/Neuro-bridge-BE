package com.nqt.jobPost_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.event.NotificationEvent;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.common_starter.kafka.NotificationEventPublisher;
import com.nqt.jobPost_service.dto.request.ApplyJobPostRequest;
import com.nqt.jobPost_service.dto.response.ApplyJobPostResponse;
import com.nqt.jobPost_service.dto.response.ExpertApplyResponse;
import com.nqt.jobPost_service.dto.response.ProposalResponse;
import com.nqt.jobPost_service.dto.response.ProposalUnlockResponse;
import com.nqt.jobPost_service.entity.ExpertApply;
import com.nqt.jobPost_service.entity.JobPost;
import com.nqt.jobPost_service.entity.Proposal;
import com.nqt.jobPost_service.entity.ProposalUnlock;
import com.nqt.jobPost_service.httpclient.IdentityServiceClient;
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

import java.time.LocalDateTime;
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
    ProposalUnlockMapper proposalUnlockMapper;
    IdentityServiceClient identityServiceClient;   // MỚI
    NotificationEventPublisher eventPublisher;

    @Override
    public void appyJobPost(ApplyJobPostRequest request) {

        // 1. Create Expert Apply
        ExpertApplyResponse expertApplyResponse =
                expertApplyService.create(request.getExpertApplyRequest());

        if (request.getProposalRequest() == null) {
            throw new GlobalException(
                    ErrorCode.DATA_INVALID.getCode(),
                    "Proposal request is required"
            );
        }

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

        // 4. Thông báo cho Client biết có Expert mới apply — MỚI
        notifyJobPostApplied(request.getExpertApplyRequest().getJobPostId(), expertApplyResponse);
    }

    private void notifyJobPostApplied(UUID jobPostId, ExpertApplyResponse expertApplyResponse) {
        try {
            JobPost jobPost = jobPostRepo.findById(jobPostId)
                    .orElseThrow(() -> new GlobalException(
                            ErrorCode.NOT_FOUND.getCode(),
                            "Job Post " + ErrorCode.NOT_FOUND.getMessage()
                    ));

            UUID clientUserId = UUID.fromString(
                    identityServiceClient.getUserByCode(jobPost.getCreatorCode()).getId()
            );
            String expertName = identityServiceClient
                    .getUserByCode(expertApplyResponse.getExpertCode())
                    .getFullName();

            eventPublisher.publish(
                    NotificationEventPublisher.JOBPOST_APPLIED,
                    NotificationEvent.builder()
                            .eventType("JOBPOST_APPLIED")
                            .domain("jobpost")
                            .recipientUserId(clientUserId)
                            .senderName(expertName)
                            .resourceId(jobPostId)
                            .occurredAt(LocalDateTime.now())
                            .build()
            );
        } catch (Exception e) {
            // Không rollback việc apply nếu bắn noti lỗi — chỉ log lại
            log.error("Failed to send JOBPOST_APPLIED notification for jobPost {}: {}",
                    jobPostId, e.getMessage());
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
                proposalUnlockResponse = proposalUnlockMapper.toResponse(proposalUnlock);
            }
        }

        return ApplyJobPostResponse.builder()
                .expertApply(expertApplyResponse)
                .proposal(proposalResponse)
                .proposalUnlock(proposalUnlockResponse)
                .build();
    }

}
