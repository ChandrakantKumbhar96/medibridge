package com.medibridge.common.config;

import com.medibridge.admin.AdminRepository;
import com.medibridge.admin.entity.Admin;
import com.medibridge.common.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds the demo admin.
 *
 * <p>Deliberately done here rather than in V2__seed_reference_data.sql: the
 * hash is produced by the same PasswordEncoder bean the login path verifies
 * against, so it cannot silently be wrong. A hand-written BCrypt string in SQL
 * is unverifiable.
 */
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String DEMO_ADMIN_EMAIL = "admin@medibridge.com";
    private static final String DEMO_ADMIN_PASSWORD = "Admin@123";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner seedAdmin() {
        return args -> {
            if (adminRepository.existsByEmailIgnoreCase(DEMO_ADMIN_EMAIL)) {
                return;
            }

            adminRepository.save(Admin.builder()
                    .fullName("Admin User")
                    .email(DEMO_ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode(DEMO_ADMIN_PASSWORD))
                    .title("System Administrator")
                    .status(AccountStatus.ACTIVE)
                    .build());

            log.warn("Seeded demo admin {} / {} - change this before deploying anywhere real",
                    DEMO_ADMIN_EMAIL, DEMO_ADMIN_PASSWORD);
        };
    }
}
