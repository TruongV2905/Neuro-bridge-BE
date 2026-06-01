package com.nqt.identity_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkillResponse {
    long id;
    String skillName;
    String skillDescription;
    LocalDateTime createdAt;
}
