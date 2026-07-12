package com.nqt.workspace_service.dto.response;
import com.nqt.workspace_service.constant.WorkspaceStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkspaceResponse {
    private UUID workspaceId;
    private UUID jobId;
    private UUID escrowId;
    private WorkspaceStatus status;
    private LocalDateTime createdAt;

    private MemberInfo client;
    private MemberInfo expert;

    private String projectName;
    private LocalDateTime deadline;
    private LocalDateTime startDate;
    private String projectStatus;
    private Float progressPercentage;

    @Data
    @Builder
    public static class MemberInfo {
        private UUID userId;
        private String name;
        private String avatarUrl;
    }
}
