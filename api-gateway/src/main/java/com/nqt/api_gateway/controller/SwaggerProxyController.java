package com.nqt.api_gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class SwaggerProxyController {

    @Autowired
    private WebClient.Builder webClientBuilder;


    @Value("${app.api-prefix}")
    private String apiPrefix;


    @GetMapping("/v3/api-docs/{serviceName}")
    public Mono<Map<String, Object>> getSwagger(@PathVariable String serviceName) {
        String shortName = serviceName.replace("-service", "").toLowerCase();

        // Gọi trực tiếp qua Load Balancer của Spring Cloud
        String url = "http://" + serviceName + "/v3/api-docs";

        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(swagger -> modifySwaggerJson(swagger, shortName))
                .onErrorResume(e -> {
                    log.error("Không thể lấy Swagger từ {}: {}", serviceName, e.getMessage());
                    return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service " + serviceName + " không phản hồi"));
                });
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> modifySwaggerJson(Map<String, Object> swagger, String shortName) {
        // 1. Xử lý sửa lại các đường dẫn (Paths)
        Object pathsObj = swagger.get("paths");
        if (pathsObj instanceof Map) {
            Map<String, Object> oldPaths = (Map<String, Object>) pathsObj;
            Map<String, Object> newPaths = new LinkedHashMap<>();

            oldPaths.forEach((path, details) -> {
                // Biến đổi: /login -> /api/identity/login
                String newPath = apiPrefix + shortName + path;
                newPaths.put(newPath, details);
            });

            swagger.put("paths", newPaths);
        }

        swagger.put("servers", List.of(Map.of(
                "url", "/",
                "description", "Gateway Server"
        )));

        return swagger;
    }
}