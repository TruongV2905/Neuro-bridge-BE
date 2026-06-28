package com.nqt.rating_service.dto.response;

import com.nqt.rating_service.enums.TargetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingSummaryResponse {
    UUID id;
    UUID expertId;
    TargetType targetType;
    BigDecimal averageScore;
    int totalReviews;
    int totalCompletedOrders;
    LocalDateTime lastUpdatedAt;
}
