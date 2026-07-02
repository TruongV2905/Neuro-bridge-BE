package com.nqt.jobPost_service.service.impl;

import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.jobPost_service.constant.JobPostStatus;
import com.nqt.jobPost_service.dto.request.jobPost.JobPostFilterRequest;
import com.nqt.jobPost_service.dto.request.jobPost.JobPostRequest;
import com.nqt.jobPost_service.dto.response.JobPostResponse;

import java.util.UUID;

public interface IJobPost {


    JobPostRequest analyzeJobPostByAI(JobPostRequest jobPost);


    JobPostResponse createJobPost(JobPostRequest jobPostRequest);

    JobPostResponse getJobPostById(UUID id);

    PageResponse<JobPostResponse> getAllJobPostsForAdmin(JobPostFilterRequest request);

    PageResponse<JobPostResponse> getAllJobPostsForClient(JobPostFilterRequest request);

    PageResponse<JobPostResponse> getAllJobPosts(JobPostFilterRequest request);

    JobPostResponse updateJobPost(UUID id, JobPostRequest jobPostRequest);

    void changeStatus(UUID id, JobPostStatus jobPostStatus);
}
