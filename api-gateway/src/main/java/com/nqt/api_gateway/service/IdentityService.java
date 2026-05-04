package com.nqt.api_gateway.service;

import com.nqt.api_gateway.dto.request.IntrospectRequest;
import com.nqt.api_gateway.dto.response.APIResponse;
import com.nqt.api_gateway.dto.response.IntrospectResponse;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class IdentityService {
     WebClient.Builder webClientBuilder;
    public Mono<APIResponse<IntrospectResponse>> introspectToken(String token) {
        return webClientBuilder.build()
                .post()
                .uri("lb://identity-service/auth/introspect")
                .bodyValue(new IntrospectRequest(token))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<APIResponse<IntrospectResponse>>() {})
                .doOnSubscribe(sub -> log.info("🔹 Sending introspect request to identity-service"))
                .doOnNext(res -> log.info("Introspect response: code={}, valid={}",
                        res.getCode(),
                        res.getResult() != null ? res.getResult().isValid() : null))
                .onErrorResume(e -> {
                    log.error("Error contacting identity-service: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
