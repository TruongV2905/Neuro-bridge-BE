package com.nqt.jobPost_service.controller;


import com.nqt.jobPost_service.dto.response.ContractResponse;
import com.nqt.jobPost_service.entity.Contract;
import com.nqt.jobPost_service.service.impl.IContractService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/contract")
@RequiredArgsConstructor
public class ContractController {

    private final IContractService contractService;

    @GetMapping("/{contractId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết hợp đồng")
    public ResponseEntity<ContractResponse> getById(@PathVariable UUID contractId) {
        Contract contract = contractService.getById(contractId);
        return ResponseEntity.ok(toResponse(contract));
    }

    @PatchMapping("/{contractId}/sign")
    @PreAuthorize("hasAnyRole('CLIENT', 'EXPERT')")
    @Operation(
            summary = "Ký hợp đồng",
            description = "Client hoặc Expert xác nhận đồng ý. Khi cả 2 bên đã ký, gọi lại API hire để khởi tạo workspace."
    )
    public ResponseEntity<ContractResponse> sign(@PathVariable UUID contractId) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        Contract contract = contractService.sign(contractId, currentUserId);
        return ResponseEntity.ok(toResponse(contract));
    }

    private ContractResponse toResponse(Contract contract) {
        return ContractResponse.builder()
                .id(contract.getId())
                .jobId(contract.getJobId())
                .proposalUnlockId(contract.getProposalUnlockId())
                .strategyDetail(contract.getStrategyDetail())
                .unlockCost(contract.getUnlockCost())
                .clientSigned(contract.isClientSigned())
                .expertSigned(contract.isExpertSigned())
                .status(contract.getStatus().name())
                .workspaceId(contract.getWorkspaceId())
                .build();
    }
}
