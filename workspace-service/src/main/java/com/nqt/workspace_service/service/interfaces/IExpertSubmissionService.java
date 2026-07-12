package com.nqt.workspace_service.service.interfaces;

import com.nqt.workspace_service.dto.request.SubmitPackageRequest;
import com.nqt.workspace_service.dto.response.SubmissionPackageResponse;
import java.util.UUID;

public interface IExpertSubmissionService {
    SubmissionPackageResponse submitPackage(SubmitPackageRequest request, UUID expertId);
}
