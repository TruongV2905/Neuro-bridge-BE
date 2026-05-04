package com.nqt.identity_service.repository.httpclient;

import com.nqt.common_starter.dto.response.APIResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient( name = "demo-service", path = "/test-client")
public interface DemoServiceClient {
    @PostMapping
    ResponseEntity<APIResponse<String>> testClient(@RequestBody String message);
}
