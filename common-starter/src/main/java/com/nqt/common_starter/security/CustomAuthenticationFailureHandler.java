package com.nqt.common_starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.APIResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final ObjectMapper objectMapper;
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException exception) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        // Kiểm tra loại Exception để map đúng ErrorCode bạn muốn
        if (exception instanceof DisabledException) {
            errorCode = ErrorCode.ACCOUNT_INACTIVE;
        } else if (exception instanceof LockedException) {
            errorCode = ErrorCode.ACCOUNT_BANNED;
        } else if (exception instanceof BadCredentialsException) {
            errorCode = ErrorCode.INVALID_PASSWORD;
        }

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        APIResponse<?> body = APIResponse.error(errorCode.getCode(), errorCode.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}