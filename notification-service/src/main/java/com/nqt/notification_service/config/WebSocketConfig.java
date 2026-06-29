package com.nqt.notification_service.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*")
                .addInterceptors(userIdHandshakeInterceptor())
                .setHandshakeHandler(principalHandshakeHandler())
                .withSockJS();
    }

    @Bean
    public HandshakeInterceptor userIdHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request,
                                           ServerHttpResponse response,
                                           WebSocketHandler wsHandler,
                                           Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    HttpServletRequest httpRequest = servletRequest.getServletRequest();

                    // Ưu tiên header (production qua Gateway)
                    String userId = httpRequest.getHeader("X-User-Id");

                    // Fallback: query param (test local)
                    if (userId == null || userId.isBlank()) {
                        userId = httpRequest.getParameter("userId");
                    }

                    if (userId != null && !userId.isBlank()) {
                        attributes.put("userId", userId);
                        log.info("[Neural Bridge] WS handshake userId={}", userId);
                    } else {
                        log.warn("[Neural Bridge] WS handshake thiếu X-User-Id header");
                    }
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response,
                                       WebSocketHandler wsHandler,
                                       Exception exception) {}
        };
    }

    @Bean
    public DefaultHandshakeHandler principalHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request,
                                              WebSocketHandler wsHandler,
                                              Map<String, Object> attributes) {
                String userId = (String) attributes.get("userId");
                if (userId != null) {
                    return () -> userId; // convertAndSendToUser dùng cái này
                }
                return super.determineUser(request, wsHandler, attributes);
            }
        };
    }
}