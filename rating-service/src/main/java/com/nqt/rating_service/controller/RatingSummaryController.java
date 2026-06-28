package com.nqt.rating_service.controller;

import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.rating_service.dto.response.RatingReviewResponse;
import com.nqt.rating_service.dto.response.RatingSummaryResponse;
import com.nqt.rating_service.service.RatingReviewService;
import com.nqt.rating_service.service.RatingSummaryService;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/experts")
public class RatingSummaryController {
    RatingSummaryService ratingSummaryService;
    RatingReviewService ratingReviewService;

    @GetMapping("/{expertId}/summary")
    public ResponseEntity<APIResponse<RatingSummaryResponse>> getExpertSummary(
            @PathVariable UUID expertId
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Get expert rating summary successfully",
                        ratingSummaryService.getExpertSummary(expertId)));
    }

    @GetMapping("/{expertId}/reviews")
    public ResponseEntity<APIResponse<PageResponse<RatingReviewResponse>>> getExpertReviews(
            @PathVariable UUID expertId,
            @ModelAttribute PageRequest pageRequest
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Get expert reviews successfully",
                        ratingReviewService.getVisibleReviewsByExpert(expertId, pageRequest)));
    }
}
