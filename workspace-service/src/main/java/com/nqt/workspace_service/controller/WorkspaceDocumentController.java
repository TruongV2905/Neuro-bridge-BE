package com.nqt.workspace_service.controller;

import com.nqt.workspace_service.dto.request.WorkspaceDocumentUpdateRequest;
import com.nqt.workspace_service.dto.response.WorkspaceDocumentResponse;
import com.nqt.workspace_service.service.interfaces.IWorkspaceDocumentService;
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
public class WorkspaceDocumentController {

    private final IWorkspaceDocumentService workspaceDocumentService;

    @GetMapping("/{workspaceId}/document")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy document của Workspace",
            description = "Trả về document do expert cập nhật. documentRequired = true nếu expert chưa cập nhật lần nào "
                    + "(FE dựa vào flag này để hiện modal yêu cầu cập nhật). Chặn chéo nếu User không thuộc Workspace."
    )
    public ResponseEntity<WorkspaceDocumentResponse> getDocument(@PathVariable UUID workspaceId) {
        UUID currentUserId = getCurrentUserId();
        return ResponseEntity.ok(workspaceDocumentService.getDocument(workspaceId, currentUserId));
    }

    @PutMapping("/{workspaceId}/document")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cập nhật document của Workspace",
            description = "Chỉ expert của Workspace được cập nhật. Client chỉ được xem."
    )
    public ResponseEntity<WorkspaceDocumentResponse> updateDocument(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody WorkspaceDocumentUpdateRequest request) {
        UUID currentUserId = getCurrentUserId();
        return ResponseEntity.ok(
                workspaceDocumentService.updateDocument(workspaceId, currentUserId, request.getDocument()));
    }

    private UUID getCurrentUserId() {
        return UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
    }
}