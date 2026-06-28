package com.nqt.rating_service.dto.response;

import com.nqt.rating_service.enums.ReferenceType;
import com.nqt.rating_service.enums.ReviewStatus;
import com.nqt.rating_service.enums.TargetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingReviewResponse {
    UUID id;
    UUID reviewerId;
    UUID expertId;
    UUID referenceId;
    ReferenceType referenceType;
    TargetType targetType;
    BigDecimal ratingScore;
    String comment;
    ReviewStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
    UUID deletedBy;
}
