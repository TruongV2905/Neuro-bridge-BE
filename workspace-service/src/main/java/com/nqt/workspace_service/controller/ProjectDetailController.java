package com.nqt.workspace_service.controller;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.workspace_service.dto.request.*;
import com.nqt.workspace_service.dto.response.ProjectDetailRespond;
import com.nqt.workspace_service.service.implement.ProjectDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Project Detail Controller", description = "Quản lý thông tin chi tiết, trạng thái và tiến độ của dự án trong Workspace")
public class ProjectDetailController {

    ProjectDetailService projectService;

    @GetMapping("/workspace/{workspaceId}")
    @Operation(summary = "Lấy thông tin chi tiết dự án theo Workspace ID")
    public ResponseEntity<APIResponse<ProjectDetailRespond>> getProjectDetail(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(APIResponse.response(
                HttpStatus.OK.value(),
                "Get project details successfully",
                projectService.getProjectDetail(workspaceId)
        ));
    }

    @PutMapping("/status")
    @Operation(summary = "Cập nhật trạng thái dự án (PENDING, IN_PROGRESS, DONE...)")
    public ResponseEntity<APIResponse<ProjectDetailRespond>> updateStatus(@Valid @RequestBody UpdateStateRequest req) {
        return ResponseEntity.ok(APIResponse.response(
                HttpStatus.OK.value(),
                "Update project status successfully",
                projectService.updateStatus(req)
        ));
    }

    @PutMapping("/progress")
    @Operation(summary = "Cập nhật tiến độ dự án theo phần trăm (0% - 100%)")
    public ResponseEntity<APIResponse<ProjectDetailRespond>> updateProgress(@Valid @RequestBody UpdateProgressRequest req) {
        return ResponseEntity.ok(APIResponse.response(
                HttpStatus.OK.value(),
                "Update project progress successfully",
                projectService.updateProgress(req)
        ));
    }

    @PutMapping("/info")
    @Operation(summary = "Cập nhật thông tin tổng quan của dự án (Tên, ngày bắt đầu, deadline)")
    public ResponseEntity<APIResponse<ProjectDetailRespond>> updateProjectInfo(@Valid @RequestBody UpdateProjectRequest req) {
        return ResponseEntity.ok(APIResponse.response(
                HttpStatus.OK.value(),
                "Update project information successfully",
                projectService.updateProjectInfo(req)
        ));
    }
}
