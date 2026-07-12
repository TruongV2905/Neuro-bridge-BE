package com.nqt.workspace_service.controller;

import com.nqt.workspace_service.dto.request.AddFileRequest;
import com.nqt.workspace_service.dto.request.ReviewFileRequest;
import com.nqt.workspace_service.dto.request.UpdateFileRequest;
import com.nqt.workspace_service.dto.response.DeliverableFileRespond;
import com.nqt.workspace_service.service.interfaces.IDeliverableFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/deliverable-files")
@RequiredArgsConstructor
@Tag(name = "Deliverable File Management", description = "API quản lý các tập tin sản phẩm bàn giao trong không gian làm việc (Workspace)")
public class DeliverableFileController {

    private final IDeliverableFileService fileService;

    // 1. Xem danh sách bài nộp: Chỉ cần đăng nhập hệ thống thành công (Service sẽ lo chặn chéo Workspace)
    @GetMapping("/packages/{packageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy danh sách file sản phẩm theo Mã gói nộp bài",
            description = "Trả về danh sách tập tin thuộc về một gói sản phẩm bàn giao cụ thể. Chặn chéo nếu User không thuộc Workspace."
    )
    public ResponseEntity<List<DeliverableFileRespond>> getFilesByPackage(
            @PathVariable UUID packageId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        List<DeliverableFileRespond> files = fileService.getFilesByPackage(packageId, currentUserId);
        return ResponseEntity.ok(files);
    }

    // 2. Thêm file mới: Thường phân hệ sinh viên/client nộp bài sẽ có role chung hoặc chặn sâu trong service
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đăng tải/Nộp tập tin sản phẩm mới vào gói bàn giao")
    public ResponseEntity<DeliverableFileRespond> addFile(
            @Valid @RequestBody AddFileRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getClaim("sub").toString());
        DeliverableFileRespond respond = fileService.addFile(req, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(respond);
    }

    // 3. Chỉnh sửa file
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chỉnh sửa thông tin tập tin sản phẩm đã nộp")
    public ResponseEntity<DeliverableFileRespond> updateFile(
            @Valid @RequestBody UpdateFileRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getClaim("sub").toString());
        DeliverableFileRespond respond = fileService.updateFile(req, currentUserId);
        return ResponseEntity.ok(respond);
    }

    // 4. Xóa file (Soft Delete)
    @DeleteMapping("/{fileId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gỡ bỏ tập tin sản phẩm khỏi gói bàn giao (Soft Delete)")
    public ResponseEntity<Boolean> deleteFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getClaim("sub").toString());
        boolean isDeleted = fileService.deleteFile(fileId, currentUserId);
        return ResponseEntity.ok(isDeleted);
    }


    @PatchMapping("/review")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Client review từng file")
    public ResponseEntity<DeliverableFileRespond> reviewFile(
            @Valid @RequestBody ReviewFileRequest req) {
        UUID currentUserId = UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        return ResponseEntity.ok(fileService.reviewFile(req, currentUserId));
    }
}