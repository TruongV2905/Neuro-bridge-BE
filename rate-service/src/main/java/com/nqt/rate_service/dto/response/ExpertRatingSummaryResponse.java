package com.nqt.rate_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpertRatingSummaryResponse {
    UUID expertId;
    Double averageScore;
    Long totalRatings;
}