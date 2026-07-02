package com.nqt.workspace_service.dto.response;
import com.nqt.workspace_service.enums.PackageStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SubmissionPackageResponse {
    private UUID packageId;
    private UUID workspaceId;
    private Integer version;
    private String notes;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime autoReleaseAt;
    private PackageStatus status;
    private String feedback;
    private List<FileDetails> files;

    @Data
    @Builder
    public static class FileDetails {
        private UUID fileId;
        private String fileName;
        private String fileUrl;
        private String fileSize;
        private String fileType;
    }
}
