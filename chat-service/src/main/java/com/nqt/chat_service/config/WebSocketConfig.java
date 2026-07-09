package com.nqt.chat_service.config;


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
    /**
     * Cấu hình broker để client subscribe và gửi message.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Client subscribe các destination dạng /topic/... hoặc /queue/...
        registry.enableSimpleBroker("/topic", "/queue");

        // Client gửi message lên server qua /app/...
        // Ví dụ: /app/chat.send -> @MessageMapping("/chat.send")
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix dùng cho gửi message riêng từng user
        // FE subscribe: /user/queue/messages
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Endpoint để FE connect WebSocket.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(userCodeHandshakeInterceptor())
                .setHandshakeHandler(principalHandshakeHandler())
                .withSockJS();
    }

    /**
     * Lấy userCode từ query param khi FE connect:
     * http://localhost:8080/ws/chat?userCode=EXPERT001
     */
    @Bean
    public HandshakeInterceptor userCodeHandshakeInterceptor() {
        return new HandshakeInterceptor() {

            @Override
            public boolean beforeHandshake(ServerHttpRequest request,
                                           ServerHttpResponse response,
                                           WebSocketHandler wsHandler,
                                           Map<String, Object> attributes) {

                if (request instanceof ServletServerHttpRequest servletRequest) {
                    HttpServletRequest httpRequest = servletRequest.getServletRequest();

                    String userCode = httpRequest.getParameter("userCode");

                    if (userCode != null && !userCode.isBlank()) {
                        attributes.put("userCode", userCode);
                        log.info("[Chat Service] WS handshake userCode={}", userCode);
                    } else {
                        log.warn("[Chat Service] WS handshake thiếu userCode query param");
                    }
                }

                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response,
                                       WebSocketHandler wsHandler,
                                       Exception exception) {
            }
        };
    }

    /**
     * Biến userCode thành Principal.
     *
     * Sau khi set Principal, Spring sẽ hiểu:
     * Principal.getName() = userCode
     *
     * Khi đó có thể gửi riêng bằng:
     * convertAndSendToUser(userCode, "/queue/messages", data)
     */
    @Bean
    public DefaultHandshakeHandler principalHandshakeHandler() {
        return new DefaultHandshakeHandler() {

            @Override
            protected Principal determineUser(ServerHttpRequest request,
                                              WebSocketHandler wsHandler,
                                              Map<String, Object> attributes) {

                String userCode = (String) attributes.get("userCode");

                if (userCode != null && !userCode.isBlank()) {
                    return () -> userCode;
                }

                return super.determineUser(request, wsHandler, attributes);
            }
        };
    }
}
