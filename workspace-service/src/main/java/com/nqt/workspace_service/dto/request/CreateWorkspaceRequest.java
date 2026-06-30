package com.nqt.workspace_service.dto.request;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateWorkspaceRequest {
    private UUID jobId;
    private UUID escrowId;
    private UUID clientId;
    private UUID expertId;
    private String projectName;
    private LocalDateTime deadline;
}
