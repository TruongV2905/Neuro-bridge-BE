package com.nqt.payment_service.dto.request;

import com.nqt.payment_service.constant.TransactionStatus;
import com.nqt.payment_service.constant.TransactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubPaymentTransactionFilterRequest {
    String fromUserCode;
    String toUserCode;
    String involvingUserCode;
    TransactionType type;
    TransactionStatus status;
    String referenceId;
}