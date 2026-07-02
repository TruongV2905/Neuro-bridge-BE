package com.nqt.workspace_service.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkspaceSummaryResponse {
    private UUID workspaceId;
    private String projectName;
    private String projectStatus;
    private Float progressPercentage;
    private LocalDateTime deadline;
    private PartnerInfo partner; // Hiển thị thông tin người đối tác bên kia phòng

    @Data
    @Builder
    public static class PartnerInfo {
        private String name;
        private String avatarUrl;
        private String role; // CLIENT hoặc EXPERT
    }
}
