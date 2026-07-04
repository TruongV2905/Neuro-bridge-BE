package com.nqt.payment_service.dto.request;

import com.nqt.common_starter.dto.request.PageRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
public class PaymentTransactionFilterRequest extends PageRequest {
    SubPaymentTransactionFilterRequest filter;
}