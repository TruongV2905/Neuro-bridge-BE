package com.nqt.rating_service.entity;

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
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "rating_summaries",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_rating_summary_expert_target",
                    columnNames = {"expert_id", "target_type"})
        })
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingSummary {
    @Id
    @UuidGenerator
    UUID id;

    @Column(name = "expert_id", nullable = false)
    UUID expertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    TargetType targetType;

    @Builder.Default
    @Column(nullable = false, precision = 10, scale = 4)
    BigDecimal averageScore = BigDecimal.ZERO;

    int totalReviews;

    int totalCompletedOrders;

    LocalDateTime lastUpdatedAt;
}
