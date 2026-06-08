package com.nqt.identity_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/internal")
public class IdentityInternalController {
    UserService userService;

    @GetMapping("/current-user")
    public ResponseEntity<UserResponse> currentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }
}
