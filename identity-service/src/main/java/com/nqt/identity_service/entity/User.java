package com.nqt.identity_service.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.identity_service.constant.Gender;
import com.nqt.identity_service.constant.UserStatus;
import jakarta.persistence.*;
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
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
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

    @ManyToMany
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "role_name"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    Set<Role> roles;

    @Column(unique = true)
    String phone;

    @Column(unique = true)
    String email;

    String password;

    LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<RefreshToken> refreshTokens;

}
