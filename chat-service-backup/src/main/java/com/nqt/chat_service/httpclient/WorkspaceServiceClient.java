package com.nqt.chat_service.httpclient;

import com.workspace.grpc.GetWorkspaceByIdRequest;
import com.workspace.grpc.WorkspaceResponse;
import com.workspace.grpc.WorkspaceServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceServiceClient {

    @GrpcClient("workspace-service")
    private WorkspaceServiceGrpc.WorkspaceServiceBlockingStub workspaceServiceBlockingStub;

    /**
     * Lấy thông tin workspace theo id qua gRPC.
     * Ném RuntimeException (được unwrap từ StatusRuntimeException) nếu không tìm thấy.
     */
    public WorkspaceResponse getWorkspaceById(String workspaceId) {
        return workspaceServiceBlockingStub.getWorkspaceById(
                GetWorkspaceByIdRequest.newBuilder()
                        .setWorkspaceId(workspaceId)
                        .build()
        );
    }
}