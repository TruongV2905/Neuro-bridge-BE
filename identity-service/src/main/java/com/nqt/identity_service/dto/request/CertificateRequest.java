package com.nqt.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateRequest {
    @NotBlank(message = "FIELD_REQUIRED")
    String certificateName;
    @NotBlank(message = "FIELD_REQUIRED")
    String issuingOrganization;

    @NotNull(message = "FIELD_REQUIRED")
    LocalDateTime issueDate;

    @NotNull(message = "FIELD_REQUIRED")
    LocalDateTime expirationDate;

    String credentialId;

    String credentialUrl;

    String certificateImageUrl;

    @NotNull(message = "FIELD_REQUIRED")
    String expertCode;
}
