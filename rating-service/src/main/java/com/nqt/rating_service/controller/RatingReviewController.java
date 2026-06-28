package com.nqt.rating_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.rating_service.dto.request.CreateWorkspaceReviewRequest;
import com.nqt.rating_service.dto.response.RatingReviewResponse;
import com.nqt.rating_service.service.RatingReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/reviews")
public class RatingReviewController {
    RatingReviewService ratingReviewService;

    @PostMapping("/workspaces/{workspaceId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<APIResponse<RatingReviewResponse>> createWorkspaceReview(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateWorkspaceReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.response(
                        HttpStatus.CREATED.value(),
                        "Create workspace review successfully",
                        ratingReviewService.createWorkspaceReview(workspaceId, request)));
    }

    @GetMapping("/workspaces/{workspaceId}/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<APIResponse<RatingReviewResponse>> getMyWorkspaceReview(
            @PathVariable UUID workspaceId
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Get workspace review successfully",
                        ratingReviewService.getMyWorkspaceReview(workspaceId)));
    }
}
