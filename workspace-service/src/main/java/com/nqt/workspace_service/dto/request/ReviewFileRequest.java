package com.nqt.workspace_service.dto.request;

import com.nqt.workspace_service.enums.FileStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewFileRequest(
        @NotNull UUID fileId,
        @NotNull FileStatus newStatus,  // APPROVED / REJECTED / REVISION_REQUIRED
        String feedback                 // Bắt buộc nếu REJECTED hoặc REVISION_REQUIRED
) {}
