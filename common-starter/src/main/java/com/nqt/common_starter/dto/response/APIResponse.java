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
    int code;
    String message;
    T result;

    public static <T> APIResponse<T> response(int code, String message, T result) {
        //đây là cách thường
        APIResponse apiResponse = new APIResponse();
        apiResponse.setCode(code);
        apiResponse.setMessage(message);
        apiResponse.setResult(result);
        //Đây là khi sài @Builder
        return APIResponse.<T>builder()
                .code(code)
                .message(message)
                .result(result)
                .build();
    }
}
