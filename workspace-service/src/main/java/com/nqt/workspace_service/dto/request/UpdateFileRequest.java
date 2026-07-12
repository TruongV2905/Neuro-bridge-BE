package com.nqt.workspace_service.dto.request;
import com.nqt.workspace_service.constant.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateFileRequest(
        @NotNull(message = "File ID không được để trống")
        UUID fileId,

        @NotBlank(message = "Tên file không được để trống")
        String fileName,

        @NotBlank(message = "Đường dẫn file không được để trống")
        String fileUrl,

        @NotBlank(message = "Kích thước file không được để trống")
        String fileSize,

        @NotNull(message = "Loại file không được để trống")
        FileType fileType
) {}
