package com.nqt.workspace_service.services.interfaces;
import com.nqt.workspace_service.dto.request.ResubmitPackageRequest;
import com.nqt.workspace_service.dto.request.ReviewPackageRequest;
import com.nqt.workspace_service.dto.request.SubmitPackageRequest;
import com.nqt.workspace_service.dto.response.PackageResRespond;
import com.nqt.workspace_service.dto.response.PackageRespond;

import java.util.List;
import java.util.UUID;

public interface ISubmissionPackageService {
    List<PackageRespond> getPackagesByWorkspace(UUID workspaceId, UUID currentUserId);
    PackageResRespond getPackageById(UUID packageId, UUID currentUserId);
    PackageRespond submitPackage(SubmitPackageRequest request, UUID currentUserId);
    PackageRespond approvePackage(ReviewPackageRequest request, UUID currentUserId);
    PackageRespond rejectPackage(ReviewPackageRequest request, UUID currentUserId);
    void deletePackage(UUID packageId, UUID currentUserId);
    PackageRespond resubmitPackage(ResubmitPackageRequest request, UUID currentUserId);
}
