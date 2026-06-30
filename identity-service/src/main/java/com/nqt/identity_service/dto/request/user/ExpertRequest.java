package com.nqt.identity_service.dto.request.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ExpertRequest extends BaseUserRequest {

    @NotEmpty(message = "FIELD_REQUIRED")
    Set<Long> skillIds;
}
