package com.nqt.workspace_service.entities;
import com.nqt.workspace_service.enums.PackageStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "submission_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID packageId;

    // Thiết lập mối quan hệ N-1 ngược về Workspace (Many-to-One)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private Integer version;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    private LocalDateTime autoReleaseAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageStatus status;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDelete = false;

    @OneToMany(mappedBy = "submissionPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeliverableFile> deliverableFiles = new ArrayList<>();
}
