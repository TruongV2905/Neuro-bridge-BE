package com.nqt.workspace_service.grpc;

import com.google.protobuf.Timestamp;
import com.nqt.workspace_service.dto.response.WorkspaceResponse;
import com.nqt.workspace_service.entity.Workspace;
import com.nqt.workspace_service.service.interfaces.IWorkspaceInitializationService;
import com.workspace.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class WorkspaceGrpcServiceImpl extends WorkspaceServiceGrpc.WorkspaceServiceImplBase {

    private final IWorkspaceInitializationService workspaceInitializationService;
    private final UserClientService userClientService;
    private final com.nqt.workspace_service.repository.WorkspaceRepository workspaceRepository;

    @Override
    public void createWorkspace(CreateWorkspaceRequest request,
                                StreamObserver<CreateWorkspaceResponse> responseObserver) {
        try {
            com.nqt.workspace_service.dto.request.CreateWorkspaceRequest dto =
                    new com.nqt.workspace_service.dto.request.CreateWorkspaceRequest();

            com.identity.grpc.UserResponse clientInfo = userClientService.getUserByCode(request.getClientId());
            com.identity.grpc.UserResponse expertInfo = userClientService.getUserByCode(request.getExpertId());

            UUID clientUuid = UUID.fromString(clientInfo.getId());
            UUID expertUuid = UUID.fromString(expertInfo.getId());

            dto.setJobId(UUID.fromString(request.getJobId()));
            dto.setClientId(clientUuid);
            dto.setExpertId(expertUuid);
            dto.setProjectName(request.getProjectName());
            dto.setDeadline(toLocalDateTime(request.getDeadline()));

            if (!request.getEscrowId().isEmpty()) {
                dto.setEscrowId(UUID.fromString(request.getEscrowId()));
            }

            WorkspaceResponse workspace = workspaceInitializationService.initializeWorkspace(dto);

            CreateWorkspaceResponse response = CreateWorkspaceResponse.newBuilder()
                    .setWorkspaceId(workspace.getWorkspaceId().toString())
                    .setSuccess(true)
                    .setMessage("Workspace created successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalStateException e) {
            // Workspace đã tồn tại cho job này
            log.warn("⚠️ Tạo workspace thất bại (đã tồn tại): {}", e.getMessage());
            responseObserver.onNext(CreateWorkspaceResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            // UUID.fromString bị sai định dạng
            log.error("❌ Dữ liệu gRPC không hợp lệ: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format: " + e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("❌ Lỗi không xác định khi tạo workspace", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to create workspace: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getWorkspaceById(GetWorkspaceByIdRequest request,
                                 StreamObserver<com.workspace.grpc.WorkspaceResponse> responseObserver) {
        UUID workspaceId;
        try {
            workspaceId = UUID.fromString(request.getWorkspaceId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("Invalid workspace id").asRuntimeException());
            return;
        }

        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription("Workspace not found: " + workspaceId).asRuntimeException());
            return;
        }

        com.workspace.grpc.WorkspaceResponse response = com.workspace.grpc.WorkspaceResponse.newBuilder()
                .setWorkspaceId(workspace.getWorkspaceId().toString())
                .setJobId(workspace.getJobId() != null ? workspace.getJobId().toString() : "")
                .setClientId(workspace.getClientId().toString())
                .setExpertId(workspace.getExpertId().toString())
                .setStatus(workspace.getStatus().name())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                ZoneId.of("Asia/Ho_Chi_Minh")
        );
    }
}