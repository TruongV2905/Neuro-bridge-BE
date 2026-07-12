package com.nqt.workspace_service.service.interfaces;
import com.nqt.workspace_service.dto.request.CreateWorkspaceRequest;
import com.nqt.workspace_service.dto.response.WorkspaceResponse;
import com.nqt.workspace_service.entity.Workspace;
import com.nqt.workspace_service.entity.WorkspaceMember;

public interface IWorkspaceInitializationService {
    WorkspaceResponse initializeWorkspace(CreateWorkspaceRequest request);

    // Hàm lấy Entity để Controller check quyền bảo mật
//    Workspace getWorkspaceById(UUID workspaceId);

    // Sửa lỗi 3: Khai báo thêm hàm convert này trong Interface
    WorkspaceResponse convertToResponseDto(Workspace workspace,
                                                  WorkspaceMember client,
                                                  WorkspaceMember expert);
}