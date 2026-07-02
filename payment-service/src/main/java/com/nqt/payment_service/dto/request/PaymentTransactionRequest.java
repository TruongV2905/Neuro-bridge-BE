package com.nqt.payment_service.dto.request;

import com.nqt.payment_service.constant.PaymentReferenceType;
import com.nqt.payment_service.constant.TransactionStatus;
import com.nqt.payment_service.constant.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentTransactionRequest {

    @Size(max = 50)
    String fromUserCode;

    @Size(max = 50)
    String toUserCode;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount;

    @NotNull
    TransactionType type;

    @Builder.Default
    TransactionStatus status = TransactionStatus.PENDING;

    @Size(max = 100)
    String referenceId;

    PaymentReferenceType referenceType;

    String description;
}