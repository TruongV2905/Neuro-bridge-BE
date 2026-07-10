package com.nqt.payment_service.dto.response;

import com.nqt.payment_service.constant.EscrowStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PayEscrowResult {
    UUID escrowId;
    EscrowStatus status;
    BigDecimal shortfallAmount;
    String message;
}
