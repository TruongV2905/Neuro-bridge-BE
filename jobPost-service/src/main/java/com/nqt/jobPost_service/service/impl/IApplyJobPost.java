package com.nqt.jobPost_service.service.impl;

import com.nqt.jobPost_service.dto.request.ApplyJobPostRequest;
import com.nqt.jobPost_service.dto.response.ApplyJobPostResponse;

import java.util.List;
import java.util.UUID;

public interface IApplyJobPost {
    public void appyJobPost(ApplyJobPostRequest request);
    public List<ApplyJobPostResponse> getAllApplyJobPostByIdJobPost(UUID jobPostId);
}
