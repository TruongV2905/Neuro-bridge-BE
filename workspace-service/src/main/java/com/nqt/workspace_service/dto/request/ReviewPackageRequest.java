package com.nqt.workspace_service.dto.request;
import lombok.Data;
import java.util.UUID;

@Data
public class ReviewPackageRequest {
    private UUID packageId;
    private String feedback; // Bắt buộc khi REJECTED, không bắt buộc khi APPROVED
}
