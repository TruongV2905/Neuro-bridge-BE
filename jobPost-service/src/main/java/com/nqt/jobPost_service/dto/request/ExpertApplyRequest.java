package com.nqt.jobPost_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpertApplyRequest {
    UUID jobPostId;
    BigDecimal proposedBudget;
    Integer estimatedDays;
}
