package com.nqt.jobPost_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.jobPost_service.constant.JobPostStatus;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobPostResponse {
    UUID id;
    Set<Long> categoryIds;
    String title;
    String description;
    Set<Long> requiredSkillIds;
    Set<Long> preferredSkillIds;
    BigDecimal budgetFrom;
    BigDecimal budgetTo;
    Integer estimatedDays;
    JobPostStatus status;
    Set<String> expertApplyCodes;
}
