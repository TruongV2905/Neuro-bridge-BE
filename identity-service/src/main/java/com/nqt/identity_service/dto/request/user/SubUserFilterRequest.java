package com.nqt.identity_service.dto.request.user;

import com.nqt.identity_service.constant.Gender;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.constant.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubUserFilterRequest {
        Role role;
        UserStatus status;
        Gender gender;
        String address;
        LocalDate createdFrom;
        LocalDate createdTo;
}
