package com.nqt.jobPost_service.dto.request.jobPost;

import com.nqt.jobPost_service.constant.JobPostStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubJobPostFilterRequest {
    UUID creatorId;

    Set<Long> categoryIds;

    Set<Long> requiredSkillIds;

    Set<Long> preferredSkillIds;

    BigDecimal minBudget;

    BigDecimal maxBudget;

    Integer estimatedDaysFrom;

    Integer estimatedDaysTo;

    JobPostStatus status;
}
