package com.nqt.identity_service.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ClientRequest extends BaseUserRequest {
    @NotBlank(message = "FIELD_REQUIRED")
    String companyName;

    @NotBlank(message = "FIELD_REQUIRED")
    String industry;

    @NotBlank(message = "FIELD_REQUIRED")
    String companyDescription;
}
