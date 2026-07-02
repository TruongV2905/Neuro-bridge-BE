package com.nqt.identity_service.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.validator.EnumValidator;
import com.nqt.identity_service.constant.Gender;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.constant.UserStatus;
import com.nqt.identity_service.util.IdGenerator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Column(unique = true, nullable = false, updatable = false)
    String code;

    String fullName;



    @Enumerated(EnumType.STRING)
    Gender gender;


    @Enumerated(EnumType.STRING)
    @Builder.Default
    UserStatus status = UserStatus.ACTIVE;

    String address;

    LocalDate dateOfBirth;

    @Column(columnDefinition = "TEXT")
    String avatarUrl;

    @Enumerated(EnumType.STRING)
    @EnumValidator(enumClass = Role.class, message = "INVALID_ENUM")
    Role role;

    @Column(unique = true)
    String phone;

    @Column(unique = true)
    String email;

    String password;

    @Builder.Default
    @Column(updatable = false)
    LocalDateTime createdAt = LocalDateTime.now();


    //-------------------------------------------------------------------

    @OneToOne(cascade = CascadeType.ALL,mappedBy = "user")
    ClientProfile clientProfile;

    @OneToOne(cascade = CascadeType.ALL,mappedBy = "user")
    ExpertProfile expertProfile;


    @PrePersist
    void generateCode() {
        if (code == null) {
            String prefix = switch (role) {
                case CLIENT -> "CLI";
                case EXPERT -> "EXP";
                case ADMIN -> "ADM";
            };

            code = prefix + "-" + IdGenerator.randomCode(10);
        }
    }





    //-------------------------------------------------------------
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
