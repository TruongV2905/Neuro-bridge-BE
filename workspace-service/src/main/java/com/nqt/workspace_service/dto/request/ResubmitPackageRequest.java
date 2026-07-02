package com.nqt.workspace_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResubmitPackageRequest {
    @NotNull
    private UUID currentPackageId; // Package cũ đang REJECTED/PENDING

    private String notes;

    @NotNull @Size(min = 1)
    private List<DeliverableFileRequest> updatedFiles; // Chỉ các file cần upload lại
}
