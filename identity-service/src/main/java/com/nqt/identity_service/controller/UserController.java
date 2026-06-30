package com.nqt.identity_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.identity_service.constant.UserStatus;
import com.nqt.identity_service.dto.request.user.ClientRequest;
import com.nqt.identity_service.dto.request.user.ExpertRequest;
import com.nqt.identity_service.dto.request.user.UserFilterRequest;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user")
public class UserController {
    UserService userService;

    @GetMapping("/current-user")
    public ResponseEntity<APIResponse<UserResponse>> currentUser() {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Current User",
                        userService.getCurrentUser()
                )
        );
    }

    @PostMapping("/filter")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<PageResponse<UserResponse>>> getAllUsers(
           @Valid @RequestBody UserFilterRequest request
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get users successfully",userService.getAllUsers(request))
        );
    }
    @GetMapping("/{code}")
    public ResponseEntity<APIResponse<UserResponse>> getUserByCode(
            @PathVariable String code
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get user successfully",userService.getUserByCode(code))
        );
    }
    @PutMapping("/client")
    public ResponseEntity<APIResponse<UserResponse>> updateClient(
           @Valid @RequestBody ClientRequest request
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Update client successfully",userService.updateClient(request))
        );
    }
    @PutMapping("/expert")
    public ResponseEntity<APIResponse<UserResponse>> updateExpert(
           @Valid @RequestBody ExpertRequest request
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Update expert successfully",userService.updateExpert(request))
        );
    }
    @PatchMapping("/{code}/status")
    public ResponseEntity<APIResponse<String>> changeStatus(
            @PathVariable String code,
            @RequestParam UserStatus status
    ) {
        userService.changeStatus(code, status);

        return ResponseEntity.ok(
                APIResponse.response(200,"Change status successfully",null)
        );
    }
}
