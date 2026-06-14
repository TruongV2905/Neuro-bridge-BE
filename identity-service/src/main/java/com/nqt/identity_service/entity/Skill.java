package com.nqt.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

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
    Long id;

    String skillName;

    @Column(columnDefinition = "TEXT")
    String skillDescription;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;


    boolean isDeleted;

    @ManyToMany(mappedBy = "skills")
    @Builder.Default
    Set<ExpertProfile> expert = new HashSet<>();

}
