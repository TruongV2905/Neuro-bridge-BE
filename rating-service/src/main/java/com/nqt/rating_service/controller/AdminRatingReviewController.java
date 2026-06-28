package com.nqt.rating_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.rating_service.dto.request.RatingReviewFilterRequest;
import com.nqt.rating_service.dto.response.RatingReviewResponse;
import com.nqt.rating_service.service.RatingReviewService;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRatingReviewController {
    RatingReviewService ratingReviewService;

    @GetMapping
    public ResponseEntity<APIResponse<PageResponse<RatingReviewResponse>>> getReviewsForAdmin(
            @ModelAttribute RatingReviewFilterRequest request
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Get rating reviews successfully",
                        ratingReviewService.getReviewsForAdmin(request)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<APIResponse<Void>> deleteReview(
            @PathVariable UUID reviewId
    ) {
        ratingReviewService.softDeleteReview(reviewId);

        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Delete rating review successfully",
                        null));
    }
}
