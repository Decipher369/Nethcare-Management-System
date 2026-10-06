package com.nethcare.config;

import com.nethcare.model.Role;
import com.nethcare.model.User;
import com.nethcare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/** Creates the initial production administrator; existing credentials are never reset. */
@Component
@Profile("prod")
@ConditionalOnProperty(name = "seed.accounts.enabled", havingValue = "true")
public class ProductionAccountSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ProductionAccountSeeder.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Environment environment;

    public ProductionAccountSeeder(UserRepository users, PasswordEncoder encoder, Environment environment) {
        this.users = users;
        this.encoder = encoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.existsByUsername("admin")) {
            log.info("Production admin bootstrap skipped; existing admin preserved");
            return;
        }
        String password = environment.getProperty("seed.admin.password", "");
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !password.matches("(?s).*[A-Za-z].*") || !password.matches("(?s).*\\d.*")) {
            throw new IllegalStateException("Set SEED_ADMIN_PASSWORD to a password of at least 12 characters"
                    + " with a letter and number (maximum 72 UTF-8 bytes) before enabling account seeding.");
        }
        User user = new User("admin", encoder.encode(password), Role.ADMIN);
        user.setFullName("System Administrator");
        user.setIsActive(true);
        user.setMustChangePassword(true);
        users.save(user);
        log.info("Production admin bootstrap created admin account; existing accounts preserved");
    }
}
