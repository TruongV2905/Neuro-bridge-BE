package com.nqt.jobPost_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.jobPost_service.dto.request.ApplyJobPostRequest;
import com.nqt.jobPost_service.dto.response.ApplyJobPostResponse;
import com.nqt.jobPost_service.service.ApplyJobPostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/apply-job-post")
public class ApplyJobPostController {
    ApplyJobPostService applyJobPostService;

    @PostMapping("/apply")
    public ResponseEntity<APIResponse<Void>> applyJobPost(
            @RequestBody ApplyJobPostRequest request
    ) {
        applyJobPostService.appyJobPost(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                APIResponse.response(
                        HttpStatus.CREATED.value(),
                        "Apply job post successfully",
                        null
                )
        );
    }

    @GetMapping("/{jobPostId}/applies")
    public ResponseEntity<APIResponse<List<ApplyJobPostResponse>>> getAllApplyJobPostByIdJobPost(
            @PathVariable UUID jobPostId
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Get all apply job posts successfully",
                        applyJobPostService.getAllApplyJobPostByIdJobPost(jobPostId)
                )
        );
    }
}
