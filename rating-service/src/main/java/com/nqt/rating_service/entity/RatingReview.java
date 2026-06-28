package com.nqt.rating_service.entity;

import com.nqt.rating_service.enums.ReferenceType;
import com.nqt.rating_service.enums.ReviewStatus;
import com.nqt.rating_service.enums.TargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "rating_reviews",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_rating_review_reviewer_reference",
                    columnNames = {"reviewer_id", "reference_type", "reference_id"})
        })
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingReview {
    @Id
    @UuidGenerator
    UUID id;

    @Column(name = "reviewer_id", nullable = false, updatable = false)
    UUID reviewerId;

    @Column(name = "expert_id", nullable = false, updatable = false)
    UUID expertId;

    @Column(name = "reference_id", nullable = false, updatable = false)
    UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, updatable = false, length = 30)
    ReferenceType referenceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false, length = 30)
    TargetType targetType;

    @Column(nullable = false, precision = 10, scale = 4)
    BigDecimal ratingScore;

    @Column(columnDefinition = "TEXT")
    String comment;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    ReviewStatus status = ReviewStatus.VISIBLE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    LocalDateTime deletedAt;

    UUID deletedBy;
}
