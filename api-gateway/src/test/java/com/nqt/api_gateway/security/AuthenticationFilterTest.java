package com.nqt.api_gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqt.api_gateway.config.ApiPath;
import com.nqt.api_gateway.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationFilterTest {

    @Test
    void loginAndRegisterArePublicWithoutBearerTokens() {
        AuthenticationFilter filter = filterWithPublicEndpoints("/identity/auth/**");

        assertThat(filterResponseStatus(filter, "/api/identity/auth/login", null)).isNull();
        assertThat(filterResponseStatus(filter, "/api/identity/auth/register", null)).isNull();
    }

    @Test
    void remainingConfiguredPublicRoutesAreMatchedWithoutBearerTokens() {
        AuthenticationFilter filter = filterWithPublicEndpoints(
                "/identity/skill/user",
                "/notification/api/notifications/health",
                "/notification/ws/**",
                "/chat/ws/**",
                "/jobpost/job-post/analyze-ai");

        assertThat(filterResponseStatus(filter, "/api/identity/skill/user", null)).isNull();
        assertThat(filterResponseStatus(filter, "/api/notification/api/notifications/health", null)).isNull();
        assertThat(filterResponseStatus(filter, "/api/notification/ws/notifications", null)).isNull();
        assertThat(filterResponseStatus(filter, "/api/chat/ws/connect", null)).isNull();
        assertThat(filterResponseStatus(filter, "/api/jobpost/job-post/analyze-ai", null)).isNull();
    }

    @Test
    void protectedAiAvailablePathWithoutBearerTokenReturnsUnauthorized() {
        AuthenticationFilter filter = filterWithPublicEndpoints("/identity/auth/**");

        assertThat(filterResponseStatus(filter, "/api/ai-available/v1/ai-place/expert/products", null))
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedAiAvailablePathWithBearerTokenIsForwardedUnchanged() {
        AuthenticationFilter filter = filterWithPublicEndpoints("/identity/auth/**");
        AtomicBoolean chainCalled = new AtomicBoolean();
        MockServerWebExchange exchange = exchange("/api/ai-available/v1/ai-place/expert/products", "Bearer nonblank-token");

        filter.filter(exchange, ignored -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getRequest().getHeaders().getFirst("Authorization")).isEqualTo("Bearer nonblank-token");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private AuthenticationFilter filterWithPublicEndpoints(String... publicEndpoints) {
        SecurityProperties properties = new SecurityProperties();
        properties.setPublicEndpoints(List.of(publicEndpoints));
        return new AuthenticationFilter(null, new ObjectMapper(), properties, new ApiPath("/api/"));
    }

    private HttpStatusCode filterResponseStatus(AuthenticationFilter filter, String path, String authorization) {
        MockServerWebExchange exchange = exchange(path, authorization);
        GatewayFilterChain chain = ignored -> Mono.empty();

        filter.filter(exchange, chain).block();

        return exchange.getResponse().getStatusCode();
    }

    private MockServerWebExchange exchange(String path, String authorization) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.post(path);
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return MockServerWebExchange.from(request);
    }
}
