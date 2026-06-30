package com.nqt.workspace_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class SubmitPackageRequest {
    private UUID workspaceId;
    private String notes;
    @NotNull
    @Size(min = 4, max = 4, message = "Phải có đúng 4 file")
    private List<DeliverableFileRequest> files;
    private Integer customReleaseDays;
}
