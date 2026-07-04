package com.nqt.payment_service.entity;

import com.nqt.payment_service.constant.WalletStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserWallet {
    @Id
    @UuidGenerator
    UUID id;

    String bankName;
    String accountNumber;
    String accountHolderName;
    @Builder.Default
    boolean requestWithdrawal = false;

    BigDecimal amountRequestWithdrawal;

    String userCode;
    // Số dư hiện tại trong ví
    @Column(name = "wallet_balance", precision = 19, scale = 2, nullable = false)
    BigDecimal walletBalance;

    // Tổng số tiền đã kiếm được
    @Column(name = "total_earned", precision = 19, scale = 2, nullable = false)
    BigDecimal totalEarned;

    // Tổng số tiền đã rút
    @Column(name = "total_withdrawn", precision = 19, scale = 2, nullable = false)
    BigDecimal totalWithdrawn;

    // Trạng thái ví
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    WalletStatus status;

    @PrePersist
    public void prePersist() {
        if (walletBalance == null) {
            walletBalance = BigDecimal.ZERO;
        }

        if (totalEarned == null) {
            totalEarned = BigDecimal.ZERO;
        }

        if (totalWithdrawn == null) {
            totalWithdrawn = BigDecimal.ZERO;
        }
        if(amountRequestWithdrawal == null) {
            amountRequestWithdrawal = BigDecimal.ZERO;
        }

        if (status == null) {
            status = WalletStatus.ACTIVE;
        }
    }
}
