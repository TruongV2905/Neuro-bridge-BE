package com.nqt.identity_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificateResponse {

    UUID id;

    String certificateName;

    String issuingOrganization;

    LocalDateTime issueDate;

    LocalDateTime expirationDate;

    String credentialId;

    String credentialUrl;

    String certificateImageUrl;

    LocalDateTime createdAt;
}
