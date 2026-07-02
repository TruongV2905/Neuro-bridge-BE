package com.nqt.api_gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqt.api_gateway.config.SecurityProperties;
import com.nqt.api_gateway.dto.response.APIResponse;
import com.nqt.api_gateway.service.IdentityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationFilter implements GlobalFilter, Ordered {

    IdentityService identityService;
    ObjectMapper objectMapper;
    AntPathMatcher antPathMatcher = new AntPathMatcher();
    SecurityProperties securityProperties;

    @NonFinal
    @Value("${app.api-prefix}")
    String apiPrefix;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.info("AuthenticationFilter processing path: {}", path);

        if (isPublicPath(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        List<String> authHeaders = exchange.getRequest()
                .getHeaders()
                .getOrEmpty(HttpHeaders.AUTHORIZATION);

        if (CollectionUtils.isEmpty(authHeaders)) {
            log.warn("Missing Authorization header for path: {}", path);
            return unauthenticatedResponse(exchange.getResponse());
        }

        String authHeader = authHeaders.getFirst();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header for path: {}", path);
            return unauthenticatedResponse(exchange.getResponse());
        }

        String token = authHeader.substring(7);

        return identityService.introspectToken(token)
                .flatMap(res -> {
                    if (res != null && res.getResult() != null && res.getResult().isValid()) {

                        ServerHttpRequest newRequest = exchange.getRequest().mutate()
                                .headers(headers -> {
                                    headers.remove(HttpHeaders.AUTHORIZATION);
                                    headers.setBearerAuth(token);
                                })
                                .build();

                        return chain.filter(exchange.mutate().request(newRequest).build());
                    }

                    log.warn("Invalid token for path: {}", path);
                    return unauthenticatedResponse(exchange.getResponse());
                })
                .onErrorResume(ex -> {
                    log.error("Introspection error: {}", ex.getMessage());
                    return unauthenticatedResponse(exchange.getResponse());
                });
    }

    @Override
    public int getOrder() {
        return 0;
    }

    Mono<Void> unauthenticatedResponse(ServerHttpResponse response) {
        APIResponse<?> apiResponse = APIResponse.<Object>builder()
                .code(401)
                .message("Unauthenticated")
                .build();

        String body;

        try {
            body = objectMapper.writeValueAsString(apiResponse);
        } catch (Exception e) {
            log.error("Error serializing unauthenticated response: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes()))
        );
    }

    private boolean isPublicPath(ServerHttpRequest request) {
        String path = request.getURI().getPath();

        return securityProperties.getPublicEndpoints().stream()
                .anyMatch(pattern ->
                        antPathMatcher.match(apiPrefix + pattern, path)
                );
    }
}