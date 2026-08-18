package com.nethcare.config;

import com.nethcare.model.Role;
import com.nethcare.model.User;
import com.nethcare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Puts one account of each role in the database so the rules in SecurityConfig
 * can actually be tried out. Dev profile only.
 *
 * Does nothing if any user already exists, so it will not overwrite an account
 * someone set up by hand. Passwords come from application-dev.properties.
 */
@Configuration
@Profile("dev")
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${seed.admin.password}")
    private String adminPassword;

    @Value("${seed.optician.password}")
    private String opticianPassword;

    @Value("${seed.staff.password}")
    private String staffPassword;

    @Value("${seed.surgeon.password}")
    private String surgeonPassword;

    @Value("${seed.patient.password}")
    private String patientPassword;

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("Users already present ({}), skipping seed", userRepository.count());
                return;
            }

            create(userRepository, passwordEncoder, "admin", adminPassword, Role.ADMIN,
                   "System Administrator", "admin@nethcare.lk");
            create(userRepository, passwordEncoder, "optician", opticianPassword, Role.OPTICIAN,
                   "Clinic Optician", "optician@nethcare.lk");
            create(userRepository, passwordEncoder, "staff", staffPassword, Role.STAFF_NURSE,
                   "Counter Staff", "staff@nethcare.lk");
            create(userRepository, passwordEncoder, "surgeon", surgeonPassword, Role.SURGEON,
                   "Eye Surgeon", "surgeon@nethcare.lk");
            create(userRepository, passwordEncoder, "patient", patientPassword, Role.PATIENT,
                   "Demo Patient", "patient@nethcare.lk");

            log.info("Seeded {} users (one per role)", userRepository.count());
        };
    }

    private void create(UserRepository repo, PasswordEncoder encoder, String username,
                        String rawPassword, Role role, String fullName, String email) {
        User user = new User(username, encoder.encode(rawPassword), role);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setStatus("ACTIVE");
        repo.save(user);
        log.info("Created user '{}' with role {}", username, role);
    }
}
