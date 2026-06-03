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
public class PortfolioRequest {
    @NotBlank(message = "FIELD_REQUIRED")
    String title;

    @NotBlank(message = "FIELD_REQUIRED")
    String description;

    @NotBlank(message = "FIELD_REQUIRED")
    String technologies;

    @NotNull(message = "FIELD_REQUIRED")
    LocalDateTime completedDate;

    @NotBlank(message = "FIELD_REQUIRED")
    String roleInProject;

    String projectUrl;

    @NotNull(message = "FIELD_REQUIRED")
    String expertCode;
}
