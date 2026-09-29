package com.nethcare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Login and what each role is allowed to open. Roles are set in model/Role.java.
 *
 * Paths are grouped by module so a role gets exactly its own module and
 * nothing else. Anything not listed needs a signed-in user; a path no rule
 * matches is closed rather than open.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Open to everyone
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                .requestMatchers("/api/health").permitAll()

                // M3 — the public shop front. No account needed to look at the
                // frames or find the shop's details.
                .requestMatchers("/", "/about", "/frames/**", "/contact").permitAll()

                // M1 — patient records
                .requestMatchers("/api/patients/**", "/patients/**").hasAnyRole("ADMIN", "OPTICIAN")

                // M2 — exams, prescriptions, referrals
                .requestMatchers("/api/examinations/**", "/api/prescriptions/**",
                                 "/api/referrals/**", "/examinations/**", "/referrals/**").hasAnyRole("ADMIN", "OPTICIAN", "SURGEON")

                // M3 — orders, billing, stock
                .requestMatchers("/api/orders/**", "/api/bills/**", "/api/payments/**",
                                 "/api/stock/**", "/orders/**", "/stock/**", "/bills/**")
                                 .hasAnyRole("ADMIN", "STAFF_NURSE")

                // M4 — follow-ups, reports, audit
                .requestMatchers("/api/reports/**", "/api/audit/**").hasAnyRole("ADMIN", "AUDITOR")
                .requestMatchers("/api/followups/**", "/api/clinical-advice/**").hasAnyRole("ADMIN", "OPTICIAN")
                // M4 console screens. No surgeon: the audit trail and the money
                // figures are not the clinical role's business.
                .requestMatchers("/reports/**", "/audit/**").hasAnyRole("ADMIN", "AUDITOR")
                .requestMatchers("/followups/**", "/notifications/**").hasAnyRole("ADMIN", "OPTICIAN")
                .requestMatchers("/dashboard/console").hasAnyRole("ADMIN", "OPTICIAN", "STAFF_NURSE", "AUDITOR")

                // Admin console
                .requestMatchers("/api/users/**", "/admin/**").hasRole("ADMIN")

                // Patient portal. The role check only opens the door here —
                // each query still has to filter to that patient's own id.
                .requestMatchers("/api/portal/**", "/portal/**").hasAnyRole("PATIENT", "ADMIN")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)   // then routes by role
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Kept on for form posts. Thymeleaf puts the hidden _csrf field in
            // by itself, so the login form needs no change. /api/** is skipped
            // because it is stateless and may get a non-browser client later.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
