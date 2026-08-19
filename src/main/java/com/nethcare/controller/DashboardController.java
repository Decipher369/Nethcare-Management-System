package com.nethcare.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sends each person to their own page after login.
 *
 * SecurityConfig points a successful login here, so "/" is the only place that
 * decides where someone lands.
 */
@Controller
public class DashboardController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        return "redirect:" + landingPageFor(authentication);
    }

    private String landingPageFor(Authentication authentication) {
        String role = roleOf(authentication);

        return switch (role) {
            case "ADMIN"       -> "/admin";
            case "OPTICIAN"    -> "/patients";
            case "STAFF_NURSE" -> "/orders";
            case "SURGEON"     -> "/referrals";
            case "PATIENT"     -> "/portal";
            // Should not happen — a user always has a role. Sending them to the
            // login page beats leaving them on a blank redirect.
            default -> "/login?error";
        };
    }

    private String roleOf(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            // authorities come in as ROLE_ADMIN etc
            if (authority.getAuthority().startsWith("ROLE_")) {
                return authority.getAuthority().substring(5);
            }
        }
        return "";
    }
}
