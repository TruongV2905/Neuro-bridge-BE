package com.nqt.jobPost_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractResponse {
    UUID id;
    UUID jobId;
    UUID proposalUnlockId;
    String strategyDetail;
    BigDecimal unlockCost;
    BigDecimal proposedBudget;
    boolean clientSigned;
    boolean expertSigned;
    String status;
    UUID workspaceId;
}
