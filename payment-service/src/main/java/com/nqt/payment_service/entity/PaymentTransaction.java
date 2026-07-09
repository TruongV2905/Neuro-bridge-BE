package com.nqt.payment_service.entity;

import com.nqt.payment_service.constant.PaymentReferenceType;
import com.nqt.payment_service.constant.TransactionStatus;
import com.nqt.payment_service.constant.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentTransaction {
    @Id
    @UuidGenerator
    UUID id;

    // Người bị trừ tiền, có thể null nếu là nạp tiền từ ngoài hệ thống
    @Column(name = "from_user_code", length = 50)
    String fromUserCode;

    // Người nhận tiền, có thể null nếu là rút tiền ra ngoài hệ thống
    @Column(name = "to_user_code", length = 50)
    String toUserCode;

    // Số tiền giao dịch
    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    BigDecimal amount;

    // Loại giao dịch
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    TransactionType type;

    // Trạng thái giao dịch
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    TransactionStatus status =  TransactionStatus.PENDING;

    // Id tham chiếu tới nguồn tạo giao dịch
    // Ví dụ: proposalId, aiServiceId, projectId, invoiceId, withdrawRequestId
    @Column(name = "reference_id", length = 100)
    String referenceId;

    @Enumerated(EnumType.STRING)
    PaymentReferenceType referenceType;

    // Ghi chú giao dịch
    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
