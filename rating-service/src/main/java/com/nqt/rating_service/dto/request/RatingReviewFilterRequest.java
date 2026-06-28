package com.nqt.rating_service.dto.request;

import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.rating_service.enums.ReferenceType;
import com.nqt.rating_service.enums.ReviewStatus;
import com.nqt.rating_service.enums.TargetType;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingReviewFilterRequest extends PageRequest {
    UUID reviewerId;
    UUID expertId;
    UUID referenceId;
    ReferenceType referenceType;
    TargetType targetType;
    ReviewStatus status;
}
