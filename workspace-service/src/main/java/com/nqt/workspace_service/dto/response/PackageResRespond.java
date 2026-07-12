package com.nqt.workspace_service.dto.response;
import com.nqt.workspace_service.constant.PackageStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PackageResRespond(
        UUID packageId,
        UUID workspaceId,
        Integer version,
        String notes,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        LocalDateTime autoReleaseAt,
        PackageStatus status,
        String feedback,
        Boolean isDelete,
        List<DeliverableFileRespond> deliverableFiles
) {}
