package com.nqt.common_starter.exception;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.APIResponse;
import feign.FeignException;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.Objects;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String MIN_ATTRIBUTE = "min";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<APIResponse<Object>> handleValidation(MethodArgumentNotValidException exception) {
        String enumKey = exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;
        try {
            errorCode = ErrorCode.valueOf(enumKey);

            var constraintViolation =
                    exception.getBindingResult().getAllErrors().getFirst().unwrap(ConstraintViolation.class);

            attributes = constraintViolation.getConstraintDescriptor().getAttributes();

            log.info(attributes.toString());

        } catch (IllegalArgumentException e) {

        }

        APIResponse apiResponse = new APIResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(
                Objects.nonNull(attributes)
                        ? mapAttribute(errorCode.getMessage(), attributes)
                        : errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse<Object>> handleAccessDinedException(
            AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        log.info(exception.getMessage());
        APIResponse<Object> apiResponse = new APIResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatus()).body(apiResponse);
    }

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<APIResponse<Object>> globalException(GlobalException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.info(exception.getMessage());
        APIResponse<Object> apiResponse = new APIResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(exception.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(apiResponse);
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
    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));

        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
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
