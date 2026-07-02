package com.nqt.workspace_service.entities;
import com.nqt.workspace_service.enums.ProjectState;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID projectId;
    @OneToOne
    @JoinColumn(name = "workspace_id", nullable = false, unique = true)
    private Workspace workspace;

    @Column(nullable = false)
    private String projectName;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(nullable = false)
    private LocalDateTime startDate;

    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectState status;

    @Column(nullable = false)
    private Float progressPercentage;
}
