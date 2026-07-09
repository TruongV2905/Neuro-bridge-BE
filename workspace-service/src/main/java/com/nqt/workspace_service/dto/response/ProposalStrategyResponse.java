package com.nqt.workspace_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProposalStrategyResponse {
    private UUID proposalUnlockId;
    private String strategyDetail;
    private BigDecimal unlockCost;
}
