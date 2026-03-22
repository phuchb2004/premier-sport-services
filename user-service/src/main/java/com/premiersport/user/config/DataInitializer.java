package com.premiersport.user.config;

import com.premiersport.user.entity.UserEntity;
import com.premiersport.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
        Optional<UserEntity> existing = userRepository.findByEmail(ADMIN_EMAIL);

        if (existing.isPresent()) {
            UserEntity admin = existing.get();
            boolean needsSave = false;

            // Fix password if it is not BCrypt-encoded (e.g. inserted manually)
            if (admin.getPassword() == null || !admin.getPassword().startsWith("$2")) {
                admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
                needsSave = true;
                log.info("Admin password was not BCrypt-encoded — reset to default");
            }

            // Ensure account is enabled
            if (!admin.isEnabled()) {
                admin.setEnabled(true);
                needsSave = true;
                log.info("Admin account was disabled — re-enabled");
            }

            // Ensure role is ADMIN
            if (admin.getRole() != UserEntity.Role.ADMIN) {
                admin.setRole(UserEntity.Role.ADMIN);
                needsSave = true;
                log.info("Admin account role was {} — set to ADMIN", admin.getRole());
            }

            if (needsSave) {
                userRepository.save(admin);
                log.info("Admin account repaired: {}", ADMIN_EMAIL);
            } else {
                log.info("Admin account already exists and is valid: {}", ADMIN_EMAIL);
            }
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
        log.info("Admin account created: {}", ADMIN_EMAIL);
    }
}
