package com.nqt.identity_service.dto.request.skill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkillForExpertRequest {
    @NotNull(message = "FIELD_REQUIRED")
    Set<Long> skillIds;
}

