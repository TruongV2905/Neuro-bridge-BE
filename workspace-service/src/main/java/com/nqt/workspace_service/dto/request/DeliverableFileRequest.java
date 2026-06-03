package com.nqt.workspace_service.dto.request;

import lombok.Data;

@Data
public class DeliverableFileRequest {
    private String fileName;
    private String fileUrl;
    private String fileSize;
    private String fileType; // SOURCE_CODE, DOCUMENT, VIDEO, DATASET
}
