package com.nqt.workspace_service.controllers;
import com.nqt.workspace_service.dto.request.CreateWorkspaceRequest;
import com.nqt.workspace_service.dto.response.WorkspaceResponse;
import com.nqt.workspace_service.entities.Workspace;
import com.nqt.workspace_service.services.interfaces.IWorkspaceInitializationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final IWorkspaceInitializationService workspaceService;

    @PostMapping("/initialize")
    @PreAuthorize("hasAuthority('SCOPE_internal') or hasRole('ADMIN')")
    @Operation(
            summary = "⚡ KHỞI TẠO PHÒNG LÀM VIỆC (INTERNAL ONLY)",
            description = "⚠️ **LƯU Ý:** Frontend **KHÔNG GỌI TRỰC TIẾP** API này khi User bấm nút 'Hire'. " +
                    "Luồng đi chuẩn: Frontend gọi sang `job-service` hoặc `payment-service` để cọc tiền đặt cọc (Escrow). " +
                    "Sau đó, các Service đó sẽ tự động gọi ngầm (Inter-service) sang endpoint này kèm quyền `SCOPE_internal` để đẻ ra Workspace."
    )
    public ResponseEntity<WorkspaceResponse> initializeWorkspace(@RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse response = workspaceService.initializeWorkspace(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
