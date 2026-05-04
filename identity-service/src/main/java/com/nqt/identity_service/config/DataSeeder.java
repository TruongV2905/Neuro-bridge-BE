package com.nqt.identity_service.config;

import com.nqt.common_starter.exception.GlobalException;
import com.nqt.identity_service.constant.UserStatus;
import com.nqt.identity_service.entity.Role;
import com.nqt.identity_service.entity.User;
import com.nqt.identity_service.repository.RoleRepository;
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
    RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        seedCreateRoles();
        seedCreateAdminUser();
    }
    private void seedCreateRoles(){
        // Implement logic to create roles if not exists
        if(roleRepository.count() > 0){
            return;
        }
        roleRepository.saveAll(
                java.util.List.of(
                        com.nqt.identity_service.entity.Role.builder()
                                .name("USER")
                                .description("Default role for normal users")
                                .build(),
                        com.nqt.identity_service.entity.Role.builder()
                                .name("ADMIN")
                                .description("Administrator role with full permissions")
                                .build()
                )
        );
        log.info("Seeded roles!");
    }


    private void seedCreateAdminUser() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // Implement logic to create an admin user if not exists
        if (userRepository.existsByEmail("admin@gmail.com")) {
            return;
        }
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName("ADMIN").orElseThrow(() -> new GlobalException("Admin role not found")));

        User admin =
                User.builder()
                        .email("admin@gmail.com")
                        .password(passwordEncoder.encode("Admin@123"))
                        .roles(roles)
                        .status(UserStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build();
        userRepository.save(admin);
        log.info("Seeded admin user: admin@gmail.com / Admin@123");
    }
}
