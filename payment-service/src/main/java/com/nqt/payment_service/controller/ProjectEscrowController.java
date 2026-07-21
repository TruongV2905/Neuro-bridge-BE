package com.nqt.payment_service.controller;

import com.nqt.payment_service.dto.response.ProjectEscrowResponse;
import com.nqt.payment_service.service.impl.IProjectEscrow;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/project-escrow")
@RequiredArgsConstructor
public class ProjectEscrowController {

    private final IProjectEscrow projectEscrowService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'EXPERT')")
    @Operation(
            summary = "Xem thông tin ví chung của dự án theo id",
            description = "Chỉ Client hoặc Expert liên quan tới escrow này mới xem được."
    )
    public ResponseEntity<ProjectEscrowResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectEscrowService.getByIdForUser(id));
    }
}