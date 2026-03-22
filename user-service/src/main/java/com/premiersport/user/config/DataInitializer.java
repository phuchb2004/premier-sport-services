package com.premiersport.user.config;

import com.premiersport.user.entity.UserEntity;
import com.premiersport.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL = "admin@premiersport.com";
    private static final String ADMIN_PASSWORD = "Admin@1234";

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Admin account already exists: {}", ADMIN_EMAIL);
            return;
        }

        UserEntity admin = UserEntity.builder()
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .firstName("Admin")
                .lastName("User")
                .role(UserEntity.Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);
        log.info("Admin account created: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
