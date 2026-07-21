package com.nqt.rate_service.controller;

import com.nqt.rate_service.dto.request.RatingRequest;
import com.nqt.rate_service.dto.response.ExpertRatingSummaryResponse;
import com.nqt.rate_service.dto.response.RatingResponse;
import com.nqt.rate_service.entity.Rating;
import com.nqt.rate_service.service.IRatingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rating")
@RequiredArgsConstructor
public class RatingController {

    private final IRatingService ratingService;

    @PostMapping("/{projectId}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Client đánh giá Expert sau khi dự án hoàn thành")
    public ResponseEntity<RatingResponse> rateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody RatingRequest request) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        Rating rating = ratingService.rateProject(projectId, currentUserId, request);
        return ResponseEntity.ok(toResponse(rating));
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem đánh giá của 1 dự án")
    public ResponseEntity<RatingResponse> getByProjectId(@PathVariable UUID projectId) {
        return ResponseEntity.ok(toResponse(ratingService.getByProjectId(projectId)));
    }

    @GetMapping("/expert/{expertId}/summary")
    @Operation(summary = "Xem điểm trung bình của Expert")
    public ResponseEntity<ExpertRatingSummaryResponse> getExpertSummary(@PathVariable UUID expertId) {
        return ResponseEntity.ok(ratingService.getExpertRatingSummary(expertId));
    }

    private RatingResponse toResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .projectId(rating.getProjectId())
                .clientId(rating.getClientId())
                .expertId(rating.getExpertId())
                .score(rating.getScore())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}