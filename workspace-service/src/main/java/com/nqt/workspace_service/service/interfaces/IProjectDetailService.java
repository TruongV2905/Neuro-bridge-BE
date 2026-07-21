package com.nqt.workspace_service.service.interfaces;

import com.nqt.workspace_service.dto.request.UpdateProgressRequest;
import com.nqt.workspace_service.dto.request.UpdateProjectRequest;
import com.nqt.workspace_service.dto.request.UpdateStateRequest;
import com.nqt.workspace_service.dto.response.ProjectDetailRespond;

import java.util.UUID;

public interface IProjectDetailService {
    ProjectDetailRespond getProjectDetail(UUID workspaceId);
    ProjectDetailRespond updateStatus(UpdateStateRequest req);
    ProjectDetailRespond updateProgress(UpdateProgressRequest req);
    ProjectDetailRespond updateProjectInfo(UpdateProjectRequest req);
    ProjectDetailRespond completeProject(UUID projectId);
}
