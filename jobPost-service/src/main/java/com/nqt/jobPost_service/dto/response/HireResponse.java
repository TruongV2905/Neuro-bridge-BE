package com.nqt.jobPost_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class HireResponse {
    UUID contractId;
    private UUID workspaceId;
    private UUID jobPostId;
    private String expertCode;
    private String message;
}
