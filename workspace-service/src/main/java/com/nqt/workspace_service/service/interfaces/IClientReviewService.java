package com.nqt.workspace_service.service.interfaces;

import com.nqt.workspace_service.dto.request.ReviewPackageRequest;
import com.nqt.workspace_service.dto.response.SubmissionPackageResponse;
import java.util.UUID;

public interface IClientReviewService {
    SubmissionPackageResponse approvePackage(ReviewPackageRequest request, UUID clientId);
    SubmissionPackageResponse rejectPackage(ReviewPackageRequest request, UUID clientId);
}
