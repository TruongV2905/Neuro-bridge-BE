package com.nqt.workspace_service.services.interfaces;
import com.nqt.workspace_service.dto.response.PackageResRespond;
import com.nqt.workspace_service.dto.response.PackageRespond;

import java.util.List;
import java.util.UUID;

public interface ISubmissionPackageService {
    List<PackageRespond> getPackagesByWorkspace(UUID workspaceId, UUID currentUserId);
    PackageResRespond getPackageById(UUID packageId, UUID currentUserId);
}
