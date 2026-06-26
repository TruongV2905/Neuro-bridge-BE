package com.nqt.jobPost_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Proposal {
    @Id
    @UuidGenerator
    UUID id;

    String title;

    @Column(columnDefinition = "TEXT")
    String strategySummary;

    boolean isLocked;

    @OneToOne(mappedBy = "proposal")
    ExpertApply expertApply;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "proposalUnlock_id")
    ProposalUnlock proposalUnlock;

}
