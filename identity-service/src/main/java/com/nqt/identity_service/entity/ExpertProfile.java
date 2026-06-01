package com.nqt.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
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
public class ExpertProfile implements Serializable {
    @Id
    @UuidGenerator
    @Column(name = "AI_expert_profile_id")
    UUID id;
    int completedProjects;
    float rating;


    @OneToOne(orphanRemoval = true)
    @JoinColumn(name = "user_id")
    User user;

    @ManyToMany
    @JoinTable(
            name = "expert_skill",
            joinColumns = @JoinColumn(name = "expert_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    Set<Skill> skills = new HashSet<>();


    @OneToMany(mappedBy = "expertProfile")
    @Builder.Default
    Set<Certificate> certificates = new HashSet<>();

    @OneToMany(mappedBy = "expertProfile")
    @Builder.Default
    Set<Portfolio> portfolios = new HashSet<>();
}
