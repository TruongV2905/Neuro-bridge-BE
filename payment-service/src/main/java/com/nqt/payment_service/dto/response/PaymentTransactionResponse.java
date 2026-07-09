package com.nqt.payment_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.payment_service.constant.PaymentReferenceType;
import com.nqt.payment_service.constant.TransactionStatus;
import com.nqt.payment_service.constant.TransactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentTransactionResponse {
    UUID id;
    String fromUserCode;
    String toUserCode;
    BigDecimal amount;
    TransactionType type;
    TransactionStatus status;
    String referenceId;
    PaymentReferenceType referenceType;
    String description;
    LocalDateTime createdAt;
}