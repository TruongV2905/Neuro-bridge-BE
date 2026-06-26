package com.nqt.common_starter.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.validator.EnumValidator;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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


    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<APIResponse<Object>> handleGrpcException(StatusRuntimeException ex) {

        Status.Code grpcCode = ex.getStatus().getCode();
        String grpcMessage = ex.getStatus().getDescription();

        int httpStatus = mapGrpcStatusToHttpStatus(grpcCode);

        String finalMessage = "[GRPC-IDENTITY-SERVICE]: "
                + (grpcMessage != null ? grpcMessage : "gRPC service error");

        log.error(
                "gRPC Error | gRPC Status: {} | HTTP Status: {} | Message: {}",
                grpcCode,
                httpStatus,
                grpcMessage,
                ex
        );

        APIResponse<Object> body = APIResponse.builder()
                .code(httpStatus)
                .message(finalMessage)
                .build();

        return ResponseEntity.status(httpStatus).body(body);
    }

    private int mapGrpcStatusToHttpStatus(Status.Code grpcCode) {
        return switch (grpcCode) {
            case OK -> 200;
            case INVALID_ARGUMENT -> 400;
            case UNAUTHENTICATED -> 401;
            case PERMISSION_DENIED -> 403;
            case NOT_FOUND -> 404;
            case ALREADY_EXISTS -> 409;
            case FAILED_PRECONDITION -> 400;
            case UNAVAILABLE -> 503;
            case DEADLINE_EXCEEDED -> 504;
            default -> 500;
        };
    }


}
