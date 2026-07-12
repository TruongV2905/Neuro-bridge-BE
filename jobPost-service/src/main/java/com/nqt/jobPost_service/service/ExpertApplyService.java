package com.nqt.jobPost_service.service;

import com.identity.grpc.UserResponse;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.common_starter.util.PageUtil;
import com.nqt.jobPost_service.dto.request.ExpertApplyRequest;
import com.nqt.jobPost_service.dto.response.ExpertApplyResponse;
import com.nqt.jobPost_service.entity.ExpertApply;
import com.nqt.jobPost_service.entity.JobPost;
import com.nqt.jobPost_service.httpclient.IdentityServiceClient;
import com.nqt.jobPost_service.mapper.ExpertApplyMapper;
import com.nqt.jobPost_service.repository.ExpertApplyRepo;
import com.nqt.jobPost_service.repository.JobPostRepo;
import com.nqt.jobPost_service.service.impl.IExpertApply;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ExpertApplyService implements IExpertApply {

    ExpertApplyRepo expertApplyRepo;
    JobPostRepo jobPostRepository;
    ExpertApplyMapper expertApplyMapper;
    IdentityServiceClient identityServiceClient;

    @Override
    @Transactional
    public ExpertApplyResponse create(ExpertApplyRequest request) {
        UserResponse expert = identityServiceClient.getCurrentUser();
        JobPost jobPost = jobPostRepository.findById(request.getJobPostId())
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Job Post " + ErrorCode.NOT_FOUND.getMessage()
                ));

        boolean alreadyApplied = expertApplyRepo.existsByJobPost_IdAndExpertCode(
                request.getJobPostId(),
                expert.getCode()
        );

        if (alreadyApplied) {
            throw new GlobalException(
                    ErrorCode.DATA_INVALID.getCode(),
                    "Expert đã apply vào job post này rồi"
            );
        }

        ExpertApply expertApply = expertApplyMapper.toEntity(request);
        expertApply.setExpertCode(expert.getCode());
        expertApply.setJobPost(jobPost);

        ExpertApply savedExpertApply = expertApplyRepo.save(expertApply);

        return expertApplyMapper.toResponse(savedExpertApply);
    }

    @Override
    @Transactional
    public ExpertApplyResponse update(UUID id, ExpertApplyRequest request) {
        ExpertApply expertApply = expertApplyRepo.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Expert Apply " + ErrorCode.NOT_FOUND.getMessage()
                ));

        expertApplyMapper.toUpdate(request, expertApply);

        if (request.getJobPostId() != null) {
            JobPost jobPost = jobPostRepository.findById(request.getJobPostId())
                    .orElseThrow(() -> new GlobalException(
                            ErrorCode.NOT_FOUND.getCode(),
                            "Job Post " + ErrorCode.NOT_FOUND.getMessage()
                    ));

            expertApply.setJobPost(jobPost);
        }

        ExpertApply updatedExpertApply = expertApplyRepo.save(expertApply);

        return expertApplyMapper.toResponse(updatedExpertApply);
    }

    @Override
    public ExpertApplyResponse getById(UUID id) {
        ExpertApply expertApply = expertApplyRepo.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Expert Apply " + ErrorCode.NOT_FOUND.getMessage()
                ));

        return expertApplyMapper.toResponse(expertApply);
    }

    @Override
    public PageResponse<ExpertApplyResponse> getAll(PageRequest pageRequest) {
        Pageable pageable = PageUtil.createPageable(pageRequest);

        Page<ExpertApply> expertApplyPage;

        String search = pageRequest.getSearch();

        if (search != null && !search.isBlank()) {
            expertApplyPage = expertApplyRepo.findByExpertCodeContainingIgnoreCase(search, pageable);
        } else {
            expertApplyPage = expertApplyRepo.findAll(pageable);
        }

        return PageResponse.fromPage(
                expertApplyPage.map(expertApplyMapper::toResponse)
        );
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ExpertApply expertApply = expertApplyRepo.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Expert Apply " + ErrorCode.NOT_FOUND.getMessage()
                ));

        expertApplyRepo.delete(expertApply);
    }
}