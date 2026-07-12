package com.nqt.workspace_service.entity;

import com.nqt.workspace_service.constant.WorkspaceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID workspaceId;

    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = true)
    private UUID escrowId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "expert_id", nullable = false)
    private UUID expertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // --- Mối quan hệ Composition (Toàn thể - Bộ phận) ---

    // 1-1 với ProjectDetail
    @OneToOne(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProjectDetail projectDetail;

    // 1-N với SubmissionPackage
    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmissionPackage> submissionPackages = new ArrayList<>();
}