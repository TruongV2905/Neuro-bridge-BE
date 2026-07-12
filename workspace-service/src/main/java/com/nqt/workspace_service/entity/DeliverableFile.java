package com.nqt.workspace_service.entity;
import com.nqt.workspace_service.constant.FileStatus;
import com.nqt.workspace_service.constant.FileType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deliverable_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliverableFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private SubmissionPackage submissionPackage;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 512)
    private String fileUrl;

    @Column(nullable = false)
    private String fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType fileType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDelete = false;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileStatus fileStatus = FileStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String feedback;
}
