package com.nqt.api_gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class SwaggerConfig {

    private final DiscoveryClient discoveryClient;
    private final SwaggerUiConfigParameters swaggerUiConfigParameters;
    private final RouteDefinitionWriter routeDefinitionWriter;
    private final ApplicationEventPublisher publisher;

    private final Set<String> registeredServices = new HashSet<>();


    @Scheduled(fixedRate = 10000)
    public void refreshSwagger() {
        List<String> services = discoveryClient.getServices();
        boolean hasNewService = false;

        for (String service : services) {
            String serviceName = service.toLowerCase();
            if (serviceName.equalsIgnoreCase("api-gateway") || registeredServices.contains(serviceName)) {
                continue;
            }

            String shortName = serviceName.replace("-service", "");

            // 1. Đăng ký Group Swagger
            swaggerUiConfigParameters.addGroup(serviceName, "/v3/api-docs/" + serviceName);

            // 2. Route cho Swagger JSON
            RouteDefinition swaggerRoute = new RouteDefinition();
            swaggerRoute.setId("docs_" + serviceName);
            swaggerRoute.setUri(URI.create("lb://" + serviceName));
            swaggerRoute.setPredicates(List.of(new PredicateDefinition("Path=/v3/api-docs/" + serviceName + "/**")));
            swaggerRoute.setFilters(List.of(new FilterDefinition("RewritePath=/v3/api-docs/" + serviceName + "/(?<remaining>.*), /v3/api-docs/${remaining}")));

            // 3. Route cho API thực tế
            RouteDefinition apiRoute = new RouteDefinition();
            apiRoute.setId("api_" + serviceName);
            apiRoute.setUri(URI.create("lb://" + serviceName));
            apiRoute.setPredicates(List.of(new PredicateDefinition("Path=/api/" + shortName + "/**")));
            apiRoute.setFilters(List.of(new FilterDefinition("RewritePath=/api/" + shortName + "/(?<remaining>.*), /${remaining}")));

            // 4. Lưu route
            routeDefinitionWriter.save(Mono.just(swaggerRoute)).subscribe();
            routeDefinitionWriter.save(Mono.just(apiRoute)).subscribe();

            registeredServices.add(serviceName);
            hasNewService = true;
            log.info("Successfully registered routes for: {}", serviceName);
        }

        if (hasNewService) {
            publisher.publishEvent(new RefreshRoutesEvent(this));
        }
    }
}