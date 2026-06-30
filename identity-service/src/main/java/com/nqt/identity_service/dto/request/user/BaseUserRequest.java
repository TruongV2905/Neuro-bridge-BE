package com.nqt.identity_service.dto.request.user;

import com.nqt.common_starter.validator.EnumValidator;
import com.nqt.identity_service.constant.Gender;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseUserRequest {
    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 2, max = 50, message = "INVALID_SIZE")
    String fullName;

    @NotNull(message = "FIELD_REQUIRED")
    @EnumValidator(enumClass = Gender.class)
    Gender gender;

    @NotBlank(message = "FIELD_REQUIRED")
    String address;

    @NotNull(message = "FIELD_REQUIRED")
    LocalDate dateOfBirth;

    String avatarUrl;

    @NotBlank(message = "FIELD_REQUIRED")
    @Pattern(regexp = "^(0[0-9]{9,10})$", message = "INVALID_FORMAT")
    String phone;

    @NotBlank(message = "FIELD_REQUIRED")
    @Email(message = "INVALID_FORMAT")
    String email;


}
