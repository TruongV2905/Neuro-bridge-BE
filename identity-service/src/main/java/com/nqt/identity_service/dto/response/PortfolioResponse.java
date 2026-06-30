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
public class PortfolioResponse {
    UUID id;

    String title;

    String description;

    String technologies;

    LocalDateTime completedDate;

    LocalDateTime createdAt;

    String roleInProject;

    String projectUrl;
}
