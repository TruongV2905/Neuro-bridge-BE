package com.nqt.rate_service.grpc;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.workspace.grpc.GetProjectByIdRequest;
import com.workspace.grpc.ProjectResponse;
import com.workspace.grpc.WorkspaceServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceServiceClient {

    @GrpcClient("workspace-service")
    private WorkspaceServiceGrpc.WorkspaceServiceBlockingStub workspaceStub;

    public ProjectResponse getProjectById(String projectId) {
        try {
            return workspaceStub.getProjectById(
                    GetProjectByIdRequest.newBuilder()
                            .setProjectId(projectId)
                            .build()
            );
        } catch (StatusRuntimeException e) {
            log.error("Failed to fetch project {}: {}", projectId, e.getMessage());
            throw new GlobalException(
                    ErrorCode.NOT_FOUND.getCode(),
                    "Cannot resolve project from workspace-service: " + projectId
            );
        }
    }
}
