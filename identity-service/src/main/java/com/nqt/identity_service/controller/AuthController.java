package com.nqt.identity_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.identity_service.dto.request.IntrospectRequest;
import com.nqt.identity_service.dto.request.LoginRequest;
import com.nqt.identity_service.dto.request.register.ClientRegisterRequest;
import com.nqt.identity_service.dto.request.register.ExpertRegisterRequest;
import com.nqt.identity_service.dto.response.IntrospectResponse;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.service.AuthenticationService;
import com.nqt.identity_service.service.TokenService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/auth")
public class AuthController {
    AuthenticationService authenticationService;
    TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<APIResponse<UserResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.response(
                        HttpStatus.CREATED.value(),
                        "Login Success",
                        authenticationService.login(loginRequest)
                ));
    }




    @PostMapping("/register/client")
    public ResponseEntity<APIResponse<String>> register(@Valid @RequestBody ClientRegisterRequest registerRequest) {
        authenticationService.registerClient(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.response(
                        HttpStatus.CREATED.value(),
                        "Register client successfully",
                        null
                ));
    }


    @PostMapping("/register/expert")
    public ResponseEntity<APIResponse<String>> register(@Valid @RequestBody ExpertRegisterRequest registerRequest) {
        authenticationService.registerExpert(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.response(
                        HttpStatus.CREATED.value(),
                        "Register expert successfully",
                        null
                ));
    }
    @PostMapping("/introspect")
    public ResponseEntity<APIResponse<IntrospectResponse>> introspect(@Valid @RequestBody IntrospectRequest introspectRequest) {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Introspect Success",
                        tokenService.introspect(introspectRequest)
                )
        );
    }
}
