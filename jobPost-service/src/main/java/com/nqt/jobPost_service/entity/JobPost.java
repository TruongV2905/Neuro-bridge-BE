package com.nqt.jobPost_service.entity;

import com.nqt.jobPost_service.constant.JobPostStatus;
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
public class JobPost {
    @Id
    @UuidGenerator
    UUID id;

    String creatorCode;

    @ElementCollection
    @CollectionTable(name = "job_post_categories")
    @Column(name = "category_id")
    @Builder.Default
    Set<Long> categoryIds = new HashSet<>();

    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @ElementCollection
    @CollectionTable(name = "job_post_required_skills")
    @Column(name = "skill_id")
    @Builder.Default
    Set<Long> requiredSkillIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "job_post_preferred_skills")
    @Column(name = "skill_id")
    @Builder.Default
    Set<Long> preferredSkillIds = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    BigDecimal budgetFrom;
    BigDecimal budgetTo;

    Integer estimatedDays;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    JobPostStatus status = JobPostStatus.PUBLIC;

    @OneToMany(mappedBy = "jobPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    Set<ExpertApply> expertApplies = new HashSet<>();

}
