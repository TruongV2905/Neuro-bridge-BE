package com.nqt.common_starter.exception;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.validator.EnumValidator;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Object>> handleValidation(MethodArgumentNotValidException exception){
        List<String> errors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    String enumKey = error.getDefaultMessage();

                    ErrorCode errorCode;
                    try {
                        errorCode = ErrorCode.valueOf(enumKey);
                    } catch (IllegalArgumentException e) {
                        errorCode = ErrorCode.FIELD_REQUIRED;
                    }

                    String message = error.getField() + " " + errorCode.getMessage();

                    if ("EnumValidator".equals(error.getCode())) {
                        try {
                            Field field = exception.getParameter()
                                    .getParameterType()
                                    .getDeclaredField(error.getField());

                            EnumValidator enumValidator =
                                    field.getAnnotation(EnumValidator.class);

                            if (enumValidator != null) {
                                String allowedValues = Arrays
                                        .stream(enumValidator.enumClass().getEnumConstants())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", "));

                                message += ". Allowed values: " + allowedValues;
                            }
                        } catch (NoSuchFieldException ignored) {
                        }
                    }

                    return message;
                })
                .toList();

        return ResponseEntity
                .badRequest()
                .body(APIResponse.response(400, "validation error", errors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse<Object>> handleAccessDinedException(
            AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        log.info(exception.getMessage());

        return ResponseEntity.status(errorCode.getStatus()).body(APIResponse.response(errorCode.getStatus().value(), errorCode.getMessage(), null));
    }

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<APIResponse<Object>> handleGlobal(GlobalException exception){
        int status = (exception.getCode() != null)
                ? exception.getCode() //value của code
                : HttpStatus.INTERNAL_SERVER_ERROR.value(); //500
        return ResponseEntity.status(status).body(APIResponse.response(status,exception.getMessage(),null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<Object>> httpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        APIResponse<Object> apiResponse = new APIResponse<>();
        String message = exception.getMessage();

        if (message != null && message.contains("values accepted for Enum class")) {
            int start = message.indexOf("values accepted for Enum class");
            message = message.substring(start);
        }

        // Làm sạch thêm (xóa dấu chấm thừa hoặc dòng mới)
        message = message.replaceAll("\\s+", " ").trim();
        apiResponse.setCode(ErrorCode.NOT_FOUND_ENUM.getCode());
        log.info(exception.getMessage());
        apiResponse.setMessage(message);
        return ResponseEntity.status(ErrorCode.NOT_FOUND_ENUM.getStatus()).body(apiResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIResponse<Object>> handeIllegalArgumentException(
            IllegalArgumentException exception) {
        APIResponse<Object> apiResponse = new APIResponse<>();
        apiResponse.setCode(ErrorCode.FIELDS_EMPTY.getCode());
        apiResponse.setMessage(exception.getMessage());
        return ResponseEntity.status(ErrorCode.FIELDS_EMPTY.getStatus()).body(apiResponse);
    }


    @ExceptionHandler(FeignException.class)
    public ResponseEntity<APIResponse<Object>> handleFeignException(FeignException ex) {
        // 1. Lấy tên Service từ URL
        String url = (ex.request() != null) ? ex.request().url() : "";
        String serviceName = extractServiceName(url);

        // 2. Lấy nội dung lỗi từ Service B
        String content = ex.contentUTF8();
        String downstreamMessage = extractMessage(content);

        // 3. Lấy HTTP Status
        int status = ex.status() > 0 ? ex.status() : 500;

        // 4. Build message chuyên nghiệp
        String finalMessage = String.format("[%s]: %s", serviceName, downstreamMessage);

        log.error("Feign Error | Status: {} | Service: {} | Content: {}", status, serviceName, content);

        APIResponse<Object> body = APIResponse.builder()
                .code(status)
                .message(finalMessage)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private String extractMessage(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            // Ưu tiên lấy field "message", nếu không có thì lấy "error", không có nữa thì trả về cả cục
            if (node.has("message")) return node.get("message").asText();
            if (node.has("error")) return node.get("error").asText();
            return node.toString();
        } catch (Exception e) {
            return json.isEmpty() ? "No detail message" : json;
        }
    }

    private String extractServiceName(String url) {
        if (url == null || url.isEmpty()) return "UNKNOWN";
        try {
            // Cắt chuỗi lấy host: http://demo-service/api -> DEMO-SERVICE
            return url.split("/")[2].toUpperCase();
        } catch (Exception e) {
            return "EXTERNAL-SERVICE";
        }
    }
}
