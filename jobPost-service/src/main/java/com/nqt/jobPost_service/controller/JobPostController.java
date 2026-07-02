package com.nqt.jobPost_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.jobPost_service.constant.JobPostStatus;
import com.nqt.jobPost_service.dto.request.jobPost.JobPostFilterRequest;
import com.nqt.jobPost_service.dto.request.jobPost.JobPostRequest;
import com.nqt.jobPost_service.dto.response.JobPostResponse;
import com.nqt.jobPost_service.service.JobPostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/job-post")
public class JobPostController {
    JobPostService jobPostService;

    @PostMapping("/analyze-ai")
    public ResponseEntity<APIResponse<JobPostRequest>> analyzeJobPostByAI(
            @RequestBody JobPostRequest jobPostRequest
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Analyze job post successfully", jobPostService.analyzeJobPostByAI(jobPostRequest))
        );
    }

    @PostMapping
    public ResponseEntity<APIResponse<JobPostResponse>> createJobPost(
            @RequestBody JobPostRequest jobPostRequest
    ) {
        return ResponseEntity.status(201).body(
                APIResponse.response(201, "Create job post successfully", jobPostService.createJobPost(jobPostRequest)));

    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<JobPostResponse>> getJobPostById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Get job post successfully",
                        jobPostService.getJobPostById(id)
                )
        );
    }

    @GetMapping("/admin")
    public ResponseEntity<APIResponse<PageResponse<JobPostResponse>>> getAllJobPostsForAdmin(
            @ModelAttribute JobPostFilterRequest request
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Get job posts for admin successfully",
                        jobPostService.getAllJobPostsForAdmin(request)
                )
        );
    }

    @GetMapping("/client")
    public ResponseEntity<APIResponse<PageResponse<JobPostResponse>>> getAllJobPostsForClient(
            @ModelAttribute JobPostFilterRequest request
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Get job posts for client successfully",
                        jobPostService.getAllJobPostsForClient(request)
                )
        );
    }

    @GetMapping
    public ResponseEntity<APIResponse<PageResponse<JobPostResponse>>> getAllJobPosts(
            @ModelAttribute JobPostFilterRequest request
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Get job posts successfully",
                        jobPostService.getAllJobPosts(request)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<JobPostResponse>> updateJobPost(
            @PathVariable UUID id,
            @RequestBody JobPostRequest jobPostRequest
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Update job post successfully",
                        jobPostService.updateJobPost(id, jobPostRequest)
                )
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<APIResponse<Void>> changeStatus(
            @PathVariable UUID id,
            @RequestParam JobPostStatus jobPostStatus
    ) {
        jobPostService.changeStatus(id, jobPostStatus);

        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Change job post status successfully",
                        null
                )
        );
    }


}
