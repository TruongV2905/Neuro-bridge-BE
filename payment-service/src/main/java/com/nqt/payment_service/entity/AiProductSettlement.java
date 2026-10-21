package com.nqt.payment_service.entity;

import com.nqt.payment_service.constant.AiProductSettlementState;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Durable Payment record keyed by AI Available's ProductPurchase.Id. It is not
 * an AI Available table and is the source of truth for remote reconciliation.
 */
@Entity
@Table(name = "ai_product_settlement", uniqueConstraints =
        @UniqueConstraint(name = "uk_ai_product_settlement_purchase", columnNames = "product_purchase_id"))
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiProductSettlement {

    @Id
    @UuidGenerator
    UUID id;

    @Column(name = "product_purchase_id", nullable = false, updatable = false)
    UUID productPurchaseId;

    @Column(name = "product_id", nullable = false)
    UUID productId;

    @Column(name = "client_code", nullable = false, length = 100)
    String clientCode;

    @Column(name = "expert_code", nullable = false, length = 100)
    String expertCode;

    @Column(name = "gross_amount_vnd", nullable = false, precision = 19, scale = 2)
    BigDecimal grossAmountVnd;

    @Column(name = "platform_fee_vnd", nullable = false, precision = 19, scale = 2)
    BigDecimal platformFeeVnd;

    @Column(name = "expert_net_vnd", nullable = false, precision = 19, scale = 2)
    BigDecimal expertNetVnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 50)
    AiProductSettlementState state;

    @Column(name = "main_payment_transaction_id")
    UUID mainPaymentTransactionId;

    @Column(name = "fee_payment_transaction_id")
    UUID feePaymentTransactionId;

    @Column(name = "snapshot_product_name", nullable = false, columnDefinition = "TEXT")
    String snapshotProductName;

    @Column(name = "snapshot_seller_expert_id", nullable = false)
    UUID snapshotSellerExpertId;

    @Column(name = "snapshot_seller_expert_code", nullable = false, columnDefinition = "TEXT")
    String snapshotSellerExpertCode;

    @Column(name = "snapshot_seller_display_name", nullable = false, columnDefinition = "TEXT")
    String snapshotSellerDisplayName;

    @Column(name = "shortfall_amount_vnd", precision = 19, scale = 2)
    BigDecimal shortfallAmountVnd;

    // Machine-readable, safe code only; no caught exception text is persisted.
    @Column(name = "outcome_code", length = 100)
    String outcomeCode;

    @Column(name = "settled_at_utc")
    Instant settledAtUtc;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
