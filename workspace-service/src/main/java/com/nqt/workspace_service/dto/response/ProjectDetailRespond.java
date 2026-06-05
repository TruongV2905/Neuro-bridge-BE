package com.nqt.workspace_service.dto.response;

import com.nqt.workspace_service.enums.ProjectState;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectDetailRespond {
    UUID projectId;
    UUID workspaceId;
    String projectName;
    LocalDateTime deadline;
    LocalDateTime startDate;
    LocalDateTime completedAt;
    ProjectState status;
    Float progressPercentage;
}
