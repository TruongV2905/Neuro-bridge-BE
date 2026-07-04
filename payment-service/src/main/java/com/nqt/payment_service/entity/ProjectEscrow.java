package com.nqt.payment_service.entity;

import com.nqt.payment_service.constant.EscrowStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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
public class ProjectEscrow {
    @Id
    @UuidGenerator
    UUID id;

    UUID projectId;

    String clientCode;

    String expertCode;

    UUID jobPostId;
    BigDecimal amount;
    BigDecimal heldAmount;
    BigDecimal platformFee;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    EscrowStatus status;


}
