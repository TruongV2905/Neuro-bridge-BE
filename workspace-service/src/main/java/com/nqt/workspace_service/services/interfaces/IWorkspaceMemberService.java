package com.nqt.workspace_service.services.interfaces;

import com.nqt.workspace_service.dto.response.WorkspaceMemberRespond;

import java.util.List;
import java.util.UUID;

public interface IWorkspaceMemberService {
    WorkspaceMemberRespond getWorkspaceMember(UUID userId);
    List<WorkspaceMemberRespond> getMembersByWorkspace(UUID workspaceId);
}
