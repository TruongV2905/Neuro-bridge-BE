package com.nqt.jobPost_service.dto.request.jobPost;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class JobPostRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    String title;

    @NotBlank(message = "FIELD_REQUIRED")
    String description;

    Set<Long> requiredSkillIds;

    Set<Long> preferredSkillIds;

    @NotNull(message = "FIELD_REQUIRED")
    BigDecimal budgetFrom;
    @NotNull(message = "FIELD_REQUIRED")
    BigDecimal budgetTo;
    @NotNull(message = "FIELD_REQUIRED")
    Integer estimatedDays;
}
