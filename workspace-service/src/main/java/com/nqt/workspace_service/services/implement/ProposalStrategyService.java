package com.nqt.workspace_service.services.implement;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.workspace_service.dto.response.ProposalStrategyResponse;
import com.nqt.workspace_service.entities.Workspace;
import com.nqt.workspace_service.grpc.JobPostServiceClient;
import com.nqt.workspace_service.repositories.WorkspaceRepository;
import com.nqt.workspace_service.services.interfaces.IProposalStrategyService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposalStrategyService implements IProposalStrategyService {

    private final WorkspaceRepository workspaceRepository;
    private final JobPostServiceClient jobPostServiceClient;

    @Override
    public ProposalStrategyResponse getProposalStrategy(UUID workspaceId, UUID currentUserId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Workspace " + ErrorCode.NOT_FOUND.getMessage()
                ));

        validateWorkspaceMember(workspace, currentUserId);

        try {
            var grpcResponse = jobPostServiceClient.getApprovedProposalUnlock(workspace.getJobId());
            return toResponse(grpcResponse);

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                log.info("Chưa có proposal cho jobId={}, trả về rỗng", workspace.getJobId());
                return ProposalStrategyResponse.builder()
                        .proposalUnlockId(null)
                        .strategyDetail(null)
                        .unlockCost(null)
                        .build();
            }
            throw e;
        }
    }

    @Override
    public ProposalStrategyResponse updateProposalStrategy(UUID workspaceId, UUID currentUserId, String newStrategyDetail) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Workspace " + ErrorCode.NOT_FOUND.getMessage()
                ));

        validateWorkspaceMember(workspace, currentUserId);

        try {
            var current = jobPostServiceClient.getApprovedProposalUnlock(workspace.getJobId());
            UUID proposalUnlockId = UUID.fromString(current.getProposalUnlockId());
            var updated = jobPostServiceClient.updateProposalUnlock(proposalUnlockId, newStrategyDetail);
            return toResponse(updated);

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                // ✅ Case: ExpertApply chưa có proposal -> tạo mới thay vì update
                log.info("Chưa có proposal cho jobId={}, tiến hành tạo mới", workspace.getJobId());
                var created = jobPostServiceClient.createProposalUnlockForJob(workspace.getJobId(), newStrategyDetail);
                return toResponse(created);
            }
            throw e;
        }
    }

    private ProposalStrategyResponse toResponse(com.jobpost.grpc.ProposalUnlockResponse grpcResponse) {
        return ProposalStrategyResponse.builder()
                .proposalUnlockId(UUID.fromString(grpcResponse.getProposalUnlockId()))
                .strategyDetail(grpcResponse.getStrategyDetail())
                .build();
    }

    private void validateWorkspaceMember(Workspace workspace, UUID currentUserId) {
        boolean isMember = workspace.getClientId().equals(currentUserId)
                || workspace.getExpertId().equals(currentUserId);

        if (!isMember) {
            throw new GlobalException(
                    ErrorCode.UNAUTHORIZED.getCode(),
                    "You are not a member of this workspace"
            );
        }
    }
}