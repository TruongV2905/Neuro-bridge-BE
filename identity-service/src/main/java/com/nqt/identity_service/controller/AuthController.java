package com.nqt.identity_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.identity_service.dto.request.IntrospectRequest;
import com.nqt.identity_service.dto.request.LoginRequest;
import com.nqt.identity_service.dto.request.RegisterRequest;
import com.nqt.identity_service.dto.response.IntrospectResponse;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.service.AuthenticationService;
import com.nqt.identity_service.service.TokenService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/auth")
public class AuthController {
    AuthenticationService authenticationService;
    TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<APIResponse<UserResponse>> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authenticationService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<APIResponse<String>> register(@RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authenticationService.register(registerRequest));
    }

    @PostMapping("/introspect")
    public ResponseEntity<APIResponse<IntrospectResponse>> introspect(@RequestBody IntrospectRequest introspectRequest) {
        return ResponseEntity.ok(APIResponse.success(tokenService.introspect(introspectRequest)));
    }
}
