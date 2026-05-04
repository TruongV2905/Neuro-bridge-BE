package com.nqt.identity_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.identity_service.dto.request.RoleRequest;
import com.nqt.identity_service.dto.response.RoleResponse;
import com.nqt.identity_service.service.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoleController {
    private RoleService roleService;
    @PostMapping
    public ResponseEntity<APIResponse<RoleResponse>> create(@RequestBody RoleRequest request) {
        return ResponseEntity.ok(
                APIResponse.<RoleResponse>builder()
                        .result(roleService.create(request))
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<RoleResponse>>> getAll() {
        return ResponseEntity.ok(
                APIResponse.<List<RoleResponse>>builder()
                        .result(roleService.getAll())
                        .build()
        );
    }

    @DeleteMapping("/{role}")
    public ResponseEntity<APIResponse<Void>> delete(@PathVariable String role) {
        roleService.delete(role);
        return ResponseEntity.ok(APIResponse.<Void>builder().build());
    }
}
