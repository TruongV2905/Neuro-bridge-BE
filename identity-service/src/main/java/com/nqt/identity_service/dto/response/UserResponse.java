package com.nqt.identity_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.identity_service.constant.Gender;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.constant.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    UUID id;
    String fullName;
    Gender gender;
    UserStatus status;
    String address;
    LocalDate dateOfBirth;
    Role role;
    String phone;
    String email;
    String accessToken;
    LocalDateTime createdAt;
    String refreshToken;
}
