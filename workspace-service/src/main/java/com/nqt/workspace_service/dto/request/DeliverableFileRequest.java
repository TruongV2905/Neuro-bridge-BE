package com.nqt.workspace_service.dto.request;

import com.nqt.workspace_service.enums.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public record DeliverableFileRequest(
        @NotBlank String fileName,
        @NotBlank String fileUrl,
        @NotBlank String fileSize,
        @NotNull FileType fileType
) {}
