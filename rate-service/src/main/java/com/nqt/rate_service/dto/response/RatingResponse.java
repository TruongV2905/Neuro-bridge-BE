package com.nqt.rate_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingResponse {
    UUID id;
    UUID projectId;
    UUID clientId;
    UUID expertId;
    Double score;
    String comment;
    LocalDateTime createdAt;
}