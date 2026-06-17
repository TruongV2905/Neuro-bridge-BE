package com.nqt.identity_service.dto.response.skill;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillResponse {
    long id;
    String skillName;
    String skillDescription;
    LocalDateTime createdAt;
    boolean deleted;
}
