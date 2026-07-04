package com.nqt.payment_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.payment_service.constant.WalletStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserWalletResponse {
    UUID id;

    String userCode;

    BigDecimal walletBalance;

    BigDecimal totalEarned;

    BigDecimal totalWithdrawn;

    WalletStatus status;

    String bankName;
    String accountNumber;
    String accountHolderName;
}
