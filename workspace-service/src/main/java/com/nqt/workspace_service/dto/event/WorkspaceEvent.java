package com.nqt.workspace_service.dto.event;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class WorkspaceEvent {
    private String eventType;
    private UUID workspaceId;
    private UUID escrowId;
    private UUID expertId;
    private Double amount;
}
