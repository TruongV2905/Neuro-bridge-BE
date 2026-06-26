package com.nqt.jobPost_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProposalUnlockRequest {
    String strategyDetail;
    BigDecimal unlockCost;
}
