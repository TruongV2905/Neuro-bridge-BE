package com.nqt.workspace_service.dto.response;
import com.nqt.workspace_service.constant.PackageStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record PackageRespond(
        UUID packageId,
        UUID workspaceId,
        Integer version,
        String notes,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        LocalDateTime autoReleaseAt,
        PackageStatus status,
        String feedback,
        Boolean isDelete
) {}
