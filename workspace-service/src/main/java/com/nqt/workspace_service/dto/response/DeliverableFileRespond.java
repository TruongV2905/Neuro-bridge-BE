package com.nqt.workspace_service.dto.response;
import com.nqt.workspace_service.constant.FileType;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliverableFileRespond(
        UUID fileId,
        UUID packageId,
        String fileName,
        String fileUrl,
        String fileSize,
        FileType fileType,
        LocalDateTime uploadedAt,
        boolean isDelete
) {}
