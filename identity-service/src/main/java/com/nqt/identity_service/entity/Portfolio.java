package com.nqt.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Portfolio {
    @Id
    @UuidGenerator
    @Column(name = "portfolio_id")
    UUID id;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    String technologies;

    LocalDateTime completedDate;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    String roleInProject;

    boolean isDeleted;

    @Column(columnDefinition = "TEXT")
    String projectUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_profile_id")
    ExpertProfile expertProfile;
}
