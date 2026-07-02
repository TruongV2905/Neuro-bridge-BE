package com.nqt.jobPost_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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
public class ProposalUnlock {
    @Id
    @UuidGenerator
    UUID id;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    String strategyDetail;

    BigDecimal unlockCost;

    @OneToOne(mappedBy = "proposalUnlock")
    Proposal proposal;
}
