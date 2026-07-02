package com.nqt.jobPost_service.service;

import com.identity.grpc.SkillSuggestionResponse;
import com.identity.grpc.UserResponse;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.common_starter.util.PageUtil;
import com.nqt.jobPost_service.constant.JobPostStatus;
import com.nqt.jobPost_service.dto.request.jobPost.JobPostFilterRequest;
import com.nqt.jobPost_service.dto.request.jobPost.JobPostRequest;
import com.nqt.jobPost_service.dto.request.jobPost.SubJobPostFilterRequest;
import com.nqt.jobPost_service.dto.response.JobPostResponse;
import com.nqt.jobPost_service.dto.response.NormalizedJobText;
import com.nqt.jobPost_service.entity.JobPost;
import com.nqt.jobPost_service.mapper.JobPostMapper;
import com.nqt.jobPost_service.repository.JobPostRepo;
import com.nqt.jobPost_service.httpclient.IdentityServiceClient;
import com.nqt.jobPost_service.service.impl.IJobPost;
import com.nqt.jobPost_service.specification.JobPostSpecification;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class JobPostService implements IJobPost {
    JobPostMapper jobPostMapper;
    JobPostRepo jobPostRepo;
    IdentityServiceClient identityServiceClient;
    AiService aiService;


    @Override
    public JobPostRequest analyzeJobPostByAI(JobPostRequest request) {
        //validation
        validateBudget(request);
        // 2. AI sửa title + description trước
        NormalizedJobText normalized = aiService.normalize(
                request.getTitle(),
                request.getDescription()
        );

        request.setTitle(normalized.getTitle());
        request.setDescription(normalized.getDescription());

        // 3. Gọi sang identity-service để suggest skill bằng RAG
        SkillSuggestionResponse suggestion = identityServiceClient.suggestSkills(request.getTitle(),request.getDescription());
        Set<Long> requiredSkillIds =
                new HashSet<Long>(suggestion.getRequiredSkillIdsList());

        Set<Long> preferredSkillIds =
                new HashSet<Long>(suggestion.getPreferredSkillIdsList());
        // 4. Set skill AI gợi ý vào request
        request.setRequiredSkillIds(requiredSkillIds);
        request.setPreferredSkillIds(preferredSkillIds);
        // 5. Trả request đã được AI xử lý
        return request;
    }

    private void validateBudget(JobPostRequest request) {
        if (request.getBudgetFrom().compareTo(request.getBudgetTo()) > 0) {
            throw new GlobalException("BUDGET_FROM_MUST_BE_LESS_THAN_BUDGET_TO");
        }

        if (request.getEstimatedDays() <= 0) {
            throw new GlobalException("ESTIMATED_DAYS_MUST_BE_POSITIVE");
        }
    }


    @Override
    public JobPostResponse createJobPost(JobPostRequest jobPostRequest) {
        UserResponse userResponse = identityServiceClient.getCurrentUser();
        //request to entity
        JobPost jobPost = jobPostMapper.toEntity(jobPostRequest);
        jobPost.setCreatorCode(userResponse.getCode());
        //save to DB
        jobPost = jobPostRepo.save(jobPost);
        //entity to response
        return jobPostMapper.toResponse(jobPost);
    }

    @Override
    public JobPostResponse getJobPostById(UUID id) {

            JobPost jobPost = jobPostRepo.findById(id)
                    .orElseThrow(() -> new GlobalException(
                            ErrorCode.NOT_FOUND.getCode(),
                            "Job Post " + ErrorCode.NOT_FOUND.getMessage()
                    ));

            return jobPostMapper.toResponse(jobPost);

    }

    @Override
    public PageResponse<JobPostResponse> getAllJobPostsForAdmin(JobPostFilterRequest request) {
        Pageable pageable = PageUtil.createPageable(request);

        Page<JobPost> jobPostPage = jobPostRepo.findAll(
                JobPostSpecification.filter(request),
                pageable
        );

        return PageResponse.fromPage(
                jobPostPage.map(jobPostMapper::toResponse)
        );
    }

    @Override
    public PageResponse<JobPostResponse> getAllJobPostsForClient(JobPostFilterRequest request) {
        UserResponse currentUser = identityServiceClient.getCurrentUser();

        if (request.getFilter() == null) {
            request.setFilter(new SubJobPostFilterRequest());
        }
        request.getFilter().setCreatorId(
                UUID.fromString(currentUser.getId())
        );

        Pageable pageable = PageUtil.createPageable(request);

        Page<JobPost> jobPostPage = jobPostRepo.findAll(
                JobPostSpecification.filter(request),
                pageable
        );

        return PageResponse.fromPage(
                jobPostPage.map(jobPostMapper::toResponse)
        );
    }

    @Override
    public PageResponse<JobPostResponse> getAllJobPosts(JobPostFilterRequest request) {
        if (request.getFilter() == null) {
            request.setFilter(new SubJobPostFilterRequest());
        }

        // Expert chỉ được xem PUBLIC, dù FE có truyền PRIVATE cũng bị ghi đè
        request.getFilter().setStatus(JobPostStatus.PUBLIC);

        Pageable pageable = PageUtil.createPageable(request);

        Page<JobPost> jobPostPage = jobPostRepo.findAll(
                JobPostSpecification.filter(request),
                pageable
        );

        return PageResponse.fromPage(
                jobPostPage.map(jobPostMapper::toResponse)
        );
    }

    @Override
    public JobPostResponse updateJobPost(UUID id, JobPostRequest jobPostRequest) {
        return null;
    }

    @Override
    @Transactional
    public void changeStatus(UUID id, JobPostStatus jobPostStatus) {
        JobPost jobPost = jobPostRepo.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Job Post " + ErrorCode.NOT_FOUND.getMessage()
                ));
        if (jobPostStatus == null) {
            throw new GlobalException(
                    ErrorCode.INVALID_CODE.getCode(),
                    "Job post status is required"
            );
        }
        jobPost.setStatus(jobPostStatus);
        jobPostRepo.save(jobPost);
    }
}
