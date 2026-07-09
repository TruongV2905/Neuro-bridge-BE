package com.nqt.jobPost_service.entity;

import com.nqt.jobPost_service.constant.ContractStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contracts")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    UUID jobId;

    @Column(nullable = false, unique = true)
    UUID proposalUnlockId;

    @Column(nullable = false)
    String clientId;

    @Column(nullable = false)
    String expertId;

    @Column(columnDefinition = "TEXT")
    String strategyDetail;

    @Column(precision = 19, scale = 2)
    BigDecimal unlockCost;

    @Column(nullable = false)
    String projectName;

    @Column(nullable = false)
    LocalDateTime deadline;

    @Builder.Default
    boolean clientSigned = false;

    LocalDateTime clientSignedAt;

    @Builder.Default
    boolean expertSigned = false;

    LocalDateTime expertSignedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    ContractStatus status = ContractStatus.PENDING;

    /** Set sau khi hireExpert gọi workspace-service thành công (lần gọi thứ 2) */
    UUID workspaceId;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
