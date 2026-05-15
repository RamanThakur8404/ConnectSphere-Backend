package com.connectsphere.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.connectsphere.auth.constant.AuthProvider;
import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

// DataSeeder — runs ONCE on every application startup.
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // -----------------------------------------------------------------------
    // Default admin credentials — override via environment variables in prod
    // -----------------------------------------------------------------------
    private static final String ADMIN_EMAIL    = "admin@connectsphere.com";
    private static final String ADMIN_USERNAME = "superadmin";
    private static final String ADMIN_PASSWORD = "Admin@1234"; // Change in prod!

    @Override
    public void run(String... args) {

        // Only seed if no admin with this email already exists
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            log.info("DataSeeder: Admin already exists — skipping seed.");
            return;
        }

        User admin = User.builder()
                .username(ADMIN_USERNAME)
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .fullName("Super Admin")
                .role(Role.ADMIN)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .build();

        userRepository.save(admin);

        log.info("DataSeeder: Default ADMIN created — email: {}", ADMIN_EMAIL);
        log.warn("DataSeeder: Please change the default admin password before going to production!");
    }
}