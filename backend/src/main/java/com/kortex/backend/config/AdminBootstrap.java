package com.kortex.backend.config;

import com.kortex.backend.model.Role;
import com.kortex.backend.model.User;
import com.kortex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bootstraps an initial ADMIN user on application startup if not present.
 * Controlled by properties:
 *  - admin.bootstrap.enabled
 *  - admin.bootstrap.email
 *  - admin.bootstrap.password
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${admin.bootstrap.email:admin@kortex.local}")
    private String adminEmail;

    @Value("${admin.bootstrap.password:Admin@12345!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            log.info("Admin bootstrap disabled");
            return;
        }

        try {
            if (userRepository.existsByEmail(adminEmail)) {
                log.info("Admin bootstrap skipped: user with email {} already exists", adminEmail);
                return;
            }

            User admin = User.builder()
                    .email(adminEmail)
                    .name("System Admin")
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .active(true)
                    .build();

            userRepository.save(admin);
            log.warn("Admin user created: {} (remember to change password)", adminEmail);
        } catch (Exception e) {
            log.error("Failed to bootstrap admin user: {}", e.getMessage(), e);
        }
    }
}
