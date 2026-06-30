package com.nqt.workspace_service.controllers;
import com.nqt.workspace_service.dto.request.ResubmitPackageRequest;
import com.nqt.workspace_service.dto.request.ReviewPackageRequest;
import com.nqt.workspace_service.dto.request.SubmitPackageRequest;
import com.nqt.workspace_service.dto.response.PackageResRespond;
import com.nqt.workspace_service.dto.response.PackageRespond;
import com.nqt.workspace_service.services.interfaces.ISubmissionPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/submission-packages")
@RequiredArgsConstructor
public class SubmissionPackageController {

    private final ISubmissionPackageService packageService;

    private UUID getCurrentUserId() {
        return UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
    }

    @GetMapping("/workspaces/{workspaceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PackageRespond>> getPackagesByWorkspace(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(packageService.getPackagesByWorkspace(workspaceId, getCurrentUserId()));
    }

    @GetMapping("/{packageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PackageResRespond> getPackageById(@PathVariable UUID packageId) {
        return ResponseEntity.ok(packageService.getPackageById(packageId, getCurrentUserId()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PackageRespond> submitPackage(@RequestBody SubmitPackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packageService.submitPackage(request, getCurrentUserId()));
    }

    @PostMapping("/{packageId}/resubmit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PackageRespond> resubmitPackage(
            @PathVariable UUID packageId,
            @RequestBody ResubmitPackageRequest request) {
        request.setCurrentPackageId(packageId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packageService.resubmitPackage(request, getCurrentUserId()));
    }

    @PatchMapping("/{packageId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PackageRespond> approvePackage(
            @PathVariable UUID packageId,
            @RequestBody(required = false) ReviewPackageRequest request) {
        if (request == null) request = new ReviewPackageRequest();
        request.setPackageId(packageId);
        return ResponseEntity.ok(packageService.approvePackage(request, getCurrentUserId()));
    }

    @PatchMapping("/{packageId}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PackageRespond> rejectPackage(
            @PathVariable UUID packageId,
            @RequestBody ReviewPackageRequest request) {
        request.setPackageId(packageId);
        return ResponseEntity.ok(packageService.rejectPackage(request, getCurrentUserId()));
    }

    @DeleteMapping("/{packageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePackage(@PathVariable UUID packageId) {
        packageService.deletePackage(packageId, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
