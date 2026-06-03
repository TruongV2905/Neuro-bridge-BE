package com.nqt.workspace_service.entities;
import com.nqt.workspace_service.enums.WorkspaceRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "workspace_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "member_id")
    private UUID memberId; // ID định danh của bản ghi trong bảng trung gian này

    @Column(name = "user_id", nullable = false)
    private UUID userId; // Lấy chính xác từ Identity Service quăng sang

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace; // Mối quan hệ để link tới Workspace

    @Column(nullable = false)
    private String name;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole role;
}
