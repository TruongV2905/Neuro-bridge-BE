package com.nqt.rate_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "projectId"))
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Rating {

    @Id
    @UuidGenerator
    UUID id;

    @Column(nullable = false, unique = true)
    UUID projectId;

    @Column(nullable = false)
    UUID clientId;   // real user id, resolve trực tiếp từ workspace-service

    @Column(nullable = false)
    UUID expertId;

    @Column(nullable = false)
    Double score;

    String comment;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;
}