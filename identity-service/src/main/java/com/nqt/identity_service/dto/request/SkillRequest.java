package com.nqt.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkillRequest {
    @NotBlank(message = "FIELD_REQUIRED")
    String skillName;
    @NotBlank(message = "FIELD_REQUIRED")
    String skillDescription;
}
