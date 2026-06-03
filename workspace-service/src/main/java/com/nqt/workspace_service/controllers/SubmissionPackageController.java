package com.nqt.workspace_service.controllers;
import com.nqt.workspace_service.dto.response.PackageResRespond;
import com.nqt.workspace_service.dto.response.PackageRespond;
import com.nqt.workspace_service.services.interfaces.ISubmissionPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/submission-packages")
@RequiredArgsConstructor
public class SubmissionPackageController {

    private final ISubmissionPackageService packageService;

    @GetMapping("/workspaces/{workspaceId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy danh sách gói nộp bài theo Mã Workspace",
            description = "Trả về toàn bộ danh sách gói bàn giao thuộc một Workspace cụ thể. Hệ thống sẽ tự động chặn nếu bạn không phải thành viên."
    )
    public ResponseEntity<List<PackageRespond>> getPackagesByWorkspace(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getClaim("sub").toString());
        List<PackageRespond> packages = packageService.getPackagesByWorkspace(workspaceId, currentUserId);
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/{packageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy thông tin chi tiết gói nộp bài",
            description = "Trả về thông tin chi tiết kèm danh sách các file đính kèm của gói bàn giao đó."
    )
    public ResponseEntity<PackageResRespond> getPackageById(
            @PathVariable UUID packageId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getClaim("sub").toString());
        PackageResRespond packageDetail = packageService.getPackageById(packageId, currentUserId);
        return ResponseEntity.ok(packageDetail);
    }
}
