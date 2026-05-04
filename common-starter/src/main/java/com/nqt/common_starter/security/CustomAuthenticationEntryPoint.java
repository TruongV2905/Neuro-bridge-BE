package com.nqt.common_starter.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        // Trả 401 khi chưa xác thực hoặc token invalid
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        APIResponse<?> body =
                APIResponse.error(ErrorCode.EMPTY_TOKEN.getCode(), ErrorCode.EMPTY_TOKEN.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
