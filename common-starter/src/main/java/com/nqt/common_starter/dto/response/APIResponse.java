package com.nqt.common_starter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIResponse<T> {
    @Builder.Default
    int code = 200;
    String message;
    T result;
    Map<String, List<ErrorDetail>> errors;

    public static <T> APIResponse<T> success(T result) {
        return APIResponse.<T>builder()
                .code(200)
                .message("success")
                .result(result)
                .build();
    }

    public static <T> APIResponse<T> success(T result, String message) {
        return APIResponse.<T>builder()
                .code(200)
                .message(message)
                .result(result)
                .build();
    }

    public static <T> APIResponse<T> error(int code, String message, Map<String, List<ErrorDetail>> errors) {
        return APIResponse.<T>builder()
                .code(code)
                .message(message)
                .errors(errors)
                .build();
    }

    public static <T> APIResponse<T> error(int code, String message) {
        return APIResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}
