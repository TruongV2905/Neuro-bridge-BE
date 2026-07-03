package com.nqt.jobPost_service.controller;

import com.nqt.jobPost_service.dto.response.HireResponse;
import com.nqt.jobPost_service.service.impl.IHireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/job-posts")
@RequiredArgsConstructor
public class HireController {

    private final IHireService hireService;

    @PostMapping("/{jobPostId}/expert-applies/{expertApplyId}/hire")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<HireResponse> hire(
            @PathVariable UUID jobPostId,
            @PathVariable UUID expertApplyId
    ) {
        HireResponse response = hireService.hireExpert(jobPostId, expertApplyId);
        return ResponseEntity.ok(response);
    }
}
