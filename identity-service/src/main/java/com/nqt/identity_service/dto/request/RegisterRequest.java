package com.nqt.identity_service.dto.request;

import com.nqt.common_starter.validator.EnumValidator;
import com.nqt.identity_service.constant.Gender;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 2, max = 50, message = "INVALID_SIZE_FULL_NAME")
    String fullName;

    @NotNull(message = "FIELD_REQUIRED")
    @EnumValidator(enumClass = Gender.class)
    Gender gender;

    @NotBlank(message = "FIELD_REQUIRED")
    String address;

    @Past(message = "INVALID_DATE")
    LocalDate dateOfBirth;

    @NotBlank(message = "FIELD_REQUIRED")
    @Pattern(regexp = "^(0[0-9]{9,10})$", message = "INVALID_PHONE_NUMBER")
    String phone;

    @NotBlank(message = "FIELD_REQUIRED")
    @Email(message = "INVALID_EMAIL")
    String email;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 8, max = 50, message = "INVALID_SIZE_PASSWORD")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "INVALID_PATTERN_PASSWORD")
    String password;
}
