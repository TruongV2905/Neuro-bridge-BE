package com.nqt.identity_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.identity_service.constant.Gender;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.constant.UserStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    String code;
    String fullName;
    Gender gender;
    UserStatus status;
    String address;
    String avatarUrl;
    LocalDate dateOfBirth;
    Role role;
    String phone;
    String email;
    String accessToken;
    String refreshToken;
    LocalDateTime createdAt;


    //client
    String companyName;
    String industry;
    String companyDescription;
    //expert
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Set<Long> skillIds;
}
