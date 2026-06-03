package com.nqt.workspace_service.dto.request;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class SubmitPackageRequest {
    private UUID workspaceId;
    private String notes;
    private List<DeliverableFileRequest> files;
    private Integer customReleaseDays;
}
