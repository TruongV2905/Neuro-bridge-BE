package com.nqt.identity_service.config;

import com.nqt.common_starter.exception.GlobalException;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.constant.UserStatus;
import com.nqt.identity_service.entity.User;
import com.nqt.identity_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataSeeder implements CommandLineRunner {
    UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        seedCreateAdminUser();
    }


    private void seedCreateAdminUser() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // Implement logic to create an admin user if not exists
        if (userRepository.existsByEmail("admin@gmail.com")) {
            return;
        }

        User admin =
                User.builder()
                        .email("admin@gmail.com")
                        .password(passwordEncoder.encode("Admin@123"))
                        .role(Role.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build();
        userRepository.save(admin);
        log.info("Seeded admin user: admin@gmail.com / Admin@123");
    }
}
