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
public class Certificate {

    @Id
    @UuidGenerator
    @Column(name = "certificate_id")
    UUID id;

    String certificateName;

    String issuingOrganization;

    LocalDateTime issueDate;

    LocalDateTime expirationDate;

    String credentialId;
    boolean isDeleted;

    @Column(columnDefinition = "TEXT")
    String credentialUrl;

    @Column(columnDefinition = "TEXT")
    String certificateImageUrl;

    @CreationTimestamp
    LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_profile_id")
    ExpertProfile expertProfile;
}
