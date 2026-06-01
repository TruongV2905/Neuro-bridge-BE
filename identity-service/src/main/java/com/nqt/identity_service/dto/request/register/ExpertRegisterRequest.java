package com.nqt.identity_service.dto.request.register;

import jakarta.validation.constraints.*;
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
public class ExpertRegisterRequest extends BaseRegisterRequest {

    @NotEmpty(message = "FIELD_REQUIRED")
    Set<Long> skillIds;

}
