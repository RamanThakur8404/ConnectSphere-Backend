package com.connectsphere.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.connectsphere.auth.constant.AuthProvider;
import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.enabled:false}")
    private boolean adminSeedEnabled;

    @Value("${app.seed.admin.email:}")
    private String adminEmail;

    @Value("${app.seed.admin.username:}")
    private String adminUsername;

    @Value("${app.seed.admin.password:}")
    private String adminPassword;

    @Value("${app.seed.admin.full-name:}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        if (!adminSeedEnabled) {
            log.info("DataSeeder: Admin seed disabled.");
            return;
        }

        if (!hasRequiredAdminSeedConfig()) {
            log.warn("DataSeeder: Admin seed enabled but email, username, or password is missing. Skipping seed.");
            return;
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("DataSeeder: Admin already exists. Skipping seed.");
            return;
        }

        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName(StringUtils.hasText(adminFullName) ? adminFullName : adminUsername)
                .role(Role.ADMIN)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .build();

        userRepository.save(admin);
        log.info("DataSeeder: ADMIN created. email={}", adminEmail);
    }

    private boolean hasRequiredAdminSeedConfig() {
        return StringUtils.hasText(adminEmail)
                && StringUtils.hasText(adminUsername)
                && StringUtils.hasText(adminPassword);
    }
}
