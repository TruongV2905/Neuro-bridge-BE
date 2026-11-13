package com.nqt.api_gateway.config;

import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwaggerConfigTest {

    @Test
    void createsNormalizedApiRoutesForDiscoveredServices() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        SwaggerUiConfigParameters swaggerUi = mock(SwaggerUiConfigParameters.class);
        RouteDefinitionWriter writer = mock(RouteDefinitionWriter.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        when(discoveryClient.getServices()).thenReturn(List.of("identity-service", "ai-available-service"));
        when(writer.save(any())).thenAnswer(invocation -> ((Mono<RouteDefinition>) invocation.getArgument(0)).then());

        SwaggerConfig config = new SwaggerConfig(discoveryClient, swaggerUi, writer, publisher, new ApiPath("/api/"));
        config.refreshSwagger();

        assertThat(configuredApiRoute("identity-service", writer).getPredicates().getFirst().getArgs().get("_genkey_0"))
                .isEqualTo("/api/identity/**");
        assertThat(configuredApiRoute("ai-available-service", writer).getPredicates().getFirst().getArgs().get("_genkey_0"))
                .isEqualTo("/api/ai-available/**");
        assertThat(configuredApiRoute("identity-service", writer).getFilters().getFirst().getArgs().values())
                .noneMatch(value -> value.contains("/api//"));
    }

    private RouteDefinition configuredApiRoute(String serviceName, RouteDefinitionWriter writer) {
        return org.mockito.Mockito.mockingDetails(writer).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("save"))
                .map(invocation -> (Mono<RouteDefinition>) invocation.getArgument(0))
                .map(Mono::block)
                .filter(route -> route.getId().equals("api_" + serviceName))
                .findFirst()
                .orElseThrow();
    }
}
