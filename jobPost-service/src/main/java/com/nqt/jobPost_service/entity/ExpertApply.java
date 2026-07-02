package com.nqt.jobPost_service.entity;

import com.nqt.jobPost_service.constant.ApplyStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpertApply {
    @Id
    @UuidGenerator
    UUID id;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    BigDecimal proposedBudget;

    Integer estimatedDays;

    String expertCode;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    ApplyStatus status = ApplyStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "job_post_id")
    JobPost jobPost;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "proposal_id")
    Proposal proposal;


}
