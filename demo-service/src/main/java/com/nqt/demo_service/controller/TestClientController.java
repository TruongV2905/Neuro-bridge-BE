package com.nqt.demo_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/test-client")
public class TestClientController {
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<APIResponse<String>> testClient(@RequestBody String message) {
        return ResponseEntity.ok(APIResponse.success(message));
    }
}
