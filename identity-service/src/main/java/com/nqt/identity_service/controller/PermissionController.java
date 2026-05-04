package com.nqt.identity_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.identity_service.dto.request.PermissionRequest;
import com.nqt.identity_service.dto.response.PermissionResponse;
import com.nqt.identity_service.service.PermissionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PermissionController {

    PermissionService permissionService;

    @PostMapping
    public ResponseEntity<APIResponse<PermissionResponse>> create(@RequestBody PermissionRequest request) {
        return ResponseEntity.ok(
                APIResponse.<PermissionResponse>builder()
                        .result(permissionService.create(request))
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<PermissionResponse>>> getAll() {
        return ResponseEntity.ok(
                APIResponse.<List<PermissionResponse>>builder()
                        .result(permissionService.getAll())
                        .build()
        );
    }

    @DeleteMapping("/{permission}")
    public ResponseEntity<APIResponse<Void>> delete(@PathVariable String permission) {
        permissionService.delete(permission);
        return ResponseEntity.ok(APIResponse.<Void>builder().build());
    }
}
