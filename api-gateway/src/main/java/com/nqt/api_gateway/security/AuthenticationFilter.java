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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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
import java.util.Optional;

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
    @Value("${app.shared-secret}")
    String SHARED_SECRET;

    @NonFinal
    @Value("${app.api-prefix}")
    String apiPrefix;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.info("AuthenticationFilter processing path: {}", path);

        // 1. Kiểm tra nếu là đường dẫn Public
        if (isPublicPath(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        // 2. Lấy Token từ Header
        List<String> authHeaders = exchange.getRequest().getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);
        if (CollectionUtils.isEmpty(authHeaders)) {
            log.warn("Missing Authorization header for path: {}", path);
            return unauthenticatedResponse(exchange.getResponse());
        }

        String token = authHeaders.getFirst().replace("Bearer ", "");

        // 3. Gọi IdentityService để xác thực
        return identityService.introspectToken(token)
                .flatMap(res -> {
                    if (res != null && res.getResult() != null && res.getResult().isValid()) {
                        var result = res.getResult();

                        // Lấy dữ liệu người dùng
                        var userId = result.getUserId();
                        var roles = String.join(",", Optional.ofNullable(result.getRoles()).orElse(List.of()));
                        var permissions = String.join(",", Optional.ofNullable(result.getPermissions()).orElse(List.of()));
                        String dataToSign = userId + roles + permissions;

                        // Ký HMAC để xác thực header (chống giả mạo)
                        byte[] decodedKey = Base64.decodeBase64(SHARED_SECRET);
                        String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, decodedKey)
                                .hmacHex(userId + roles + permissions);
                        // Tạo request mới kèm header xác thực
                        ServerHttpRequest newRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", userId)
                                .header("X-User-Roles", roles)
                                .header("X-User-Permissions", permissions)
                                .header("X-Signature", signature)
                                .build();

                        // Tiếp tục chain với request mới
                        return chain.filter(exchange.mutate().request(newRequest).build());
                    } else {
                        log.warn("Invalid token for path: {}", path);
                        return unauthenticatedResponse(exchange.getResponse());
                    }

                })
                // Nếu gọi service xác thực bị lỗi (503, 404...), trả về 401 luôn
                .onErrorResume(ex -> {
                    log.error("Introspection error: {}", ex.getMessage());
                    return unauthenticatedResponse(exchange.getResponse());
                });
    }

    @Override
    public int getOrder() {
        return 0;
    }

    Mono<Void> unauthenticatedResponse(ServerHttpResponse response){
        APIResponse<?> apiResponse = APIResponse.<Object>builder()
                .code(401)
                .message("Unauthenticated")
                .build();
        String body = null;
        try {
            body = objectMapper.writeValueAsString(apiResponse);
        }catch (Exception e){
            log.error("Error serializing unauthenticated response: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    private boolean isPublicPath(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        log.info("API prefix loaded: {}", apiPrefix);
        log.info(securityProperties.getPublicEndpoints().toString());
        // Kiểm tra trực tiếp các đường dẫn public defined sẵn
        return securityProperties.getPublicEndpoints().stream()
                .anyMatch(pattern ->
                        antPathMatcher.match(apiPrefix + pattern, path)
                );
    }
}
