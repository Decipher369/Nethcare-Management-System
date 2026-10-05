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
import java.util.List;

/** Explicit production account bootstrap; existing credentials are never reset. */
@Component
@Profile("prod")
@ConditionalOnProperty(name = "seed.accounts.enabled", havingValue = "true")
public class ProductionAccountSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ProductionAccountSeeder.class);
    private record Account(String username, Role role, String fullName) { }
    private static final List<Account> ACCOUNTS = List.of(
            new Account("admin", Role.ADMIN, "System Administrator"),
            new Account("optician", Role.OPTICIAN, "Clinic Optician"),
            new Account("staff", Role.STAFF_NURSE, "Counter Staff"),
            new Account("surgeon", Role.SURGEON, "Eye Surgeon"),
            new Account("patient", Role.PATIENT, "Patient Account"),
            new Account("auditor", Role.AUDITOR, "Clinical Auditor"));

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
        List<Account> missing = ACCOUNTS.stream()
                .filter(account -> !users.existsByUsername(account.username())).toList();
        // Validate every missing account before inserting any rows.
        for (Account account : missing) {
            String password = passwordFor(account);
            if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72
                    || !password.matches("(?s).*[A-Za-z].*") || !password.matches("(?s).*\\d.*")) {
                throw new IllegalStateException("Set SEED_" + account.username().toUpperCase(java.util.Locale.ROOT)
                        + "_PASSWORD to a password of at least 12 characters with a letter and number"
                        + " (maximum 72 UTF-8 bytes) before enabling account seeding.");
            }
        }
        for (Account account : missing) {
            User user = new User(account.username(), encoder.encode(passwordFor(account)), account.role());
            user.setFullName(account.fullName());
            user.setIsActive(true);
            user.setMustChangePassword(true);
            users.save(user);
        }
        log.info("Production account bootstrap created {} missing accounts; existing accounts preserved", missing.size());
    }

    private String passwordFor(Account account) {
        return environment.getProperty("seed." + account.username() + ".password", "");
    }
}
