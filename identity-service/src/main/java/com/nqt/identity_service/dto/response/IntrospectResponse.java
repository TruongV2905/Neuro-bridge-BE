package com.nqt.identity_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.identity_service.constant.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntrospectResponse {
    boolean valid;
    String userId;
    String role;
}
