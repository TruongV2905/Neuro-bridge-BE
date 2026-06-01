package com.nqt.identity_service.dto.request.user;

import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.identity_service.constant.Gender;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.constant.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class UserFilterRequest extends PageRequest {
    SubUserFilterRequest filter;
}


