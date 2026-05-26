package com.nqt.identity_service.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.validator.EnumValidator;
import com.nqt.identity_service.constant.Gender;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.constant.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "users")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User implements UserDetails {
    @Id
    @UuidGenerator
    UUID id;

    String fullName;

    @Enumerated(EnumType.STRING)
    Gender gender;


    @Enumerated(EnumType.STRING)
    UserStatus status;

    String address;

    LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "FIELD_REQUIRED")
    @EnumValidator(enumClass = Role.class, message = "INVALID_ENUM")
    Role role;

    @Column(unique = true)
    String phone;

    @Column(unique = true)
    String email;

    String password;

    LocalDateTime createdAt;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != UserStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status == UserStatus.ACTIVE;
    }
}
