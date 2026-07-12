package com.nqt.workspace_service.controller;

import com.nqt.workspace_service.dto.request.ProposalStrategyUpdateRequest;
import com.nqt.workspace_service.dto.response.ProposalStrategyResponse;
import com.nqt.workspace_service.service.interfaces.IProposalStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
public class ProposalStrategyController {

    private final IProposalStrategyService proposalStrategyService;

    @GetMapping("/{workspaceId}/proposal-strategy")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy nội dung chi tiết chiến lược (Proposal Strategy) đã được duyệt của Workspace",
            description = "Trả về strategyDetail của Proposal đã được Approved. Chặn chéo nếu User không thuộc Workspace."
    )
    public ResponseEntity<ProposalStrategyResponse> getProposalStrategy(
            @PathVariable UUID workspaceId) {
        UUID currentUserId = UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        ProposalStrategyResponse response = proposalStrategyService.getProposalStrategy(workspaceId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{workspaceId}/proposal-strategy")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cập nhật nội dung chi tiết chiến lược (Proposal Strategy) của Workspace",
            description = "Chỉ cập nhật strategyDetail, giữ nguyên unlockCost. Chặn chéo nếu User không thuộc Workspace."
    )
    public ResponseEntity<ProposalStrategyResponse> updateProposalStrategy(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody ProposalStrategyUpdateRequest request) {
        UUID currentUserId = UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        ProposalStrategyResponse response = proposalStrategyService.updateProposalStrategy(
                workspaceId, currentUserId, request.getStrategyDetail());
        return ResponseEntity.ok(response);
    }
}
