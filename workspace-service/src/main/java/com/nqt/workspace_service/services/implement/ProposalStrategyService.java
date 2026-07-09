package com.nqt.workspace_service.services.implement;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.workspace_service.dto.response.ProposalStrategyResponse;
import com.nqt.workspace_service.entities.Workspace;
import com.nqt.workspace_service.grpc.JobPostServiceClient;
import com.nqt.workspace_service.repositories.WorkspaceRepository;
import com.nqt.workspace_service.services.interfaces.IProposalStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
@Service
@RequiredArgsConstructor
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

        var grpcResponse = jobPostServiceClient.getApprovedProposalUnlock(workspace.getJobId());

        return ProposalStrategyResponse.builder()
                .proposalUnlockId(UUID.fromString(grpcResponse.getProposalUnlockId()))
                .strategyDetail(grpcResponse.getStrategyDetail())
                .build();
    }

    @Override
    public ProposalStrategyResponse updateProposalStrategy(UUID workspaceId, UUID currentUserId, String newStrategyDetail) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Workspace " + ErrorCode.NOT_FOUND.getMessage()
                ));

        validateWorkspaceMember(workspace, currentUserId);

        // Lấy proposalUnlockId + unlockCost hiện tại để giữ nguyên unlockCost khi update
        var current = jobPostServiceClient.getApprovedProposalUnlock(workspace.getJobId());
        UUID proposalUnlockId = UUID.fromString(current.getProposalUnlockId());

        var updated = jobPostServiceClient.updateProposalUnlock(proposalUnlockId, newStrategyDetail);

        return toResponse(updated);
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
