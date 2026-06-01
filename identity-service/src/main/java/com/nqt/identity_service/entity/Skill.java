package com.nqt.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "skill_id")
    long id;

    String skillName;
    String skillDescription;
    @Builder.Default
    @Column(updatable = false)
    LocalDateTime createdAt = LocalDateTime.now();

    boolean isDeleted;

    @ManyToMany(mappedBy = "skills")
    @Builder.Default
    Set<ExpertProfile> expert = new HashSet<>();

}
