package com.nethcare.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sends each person to their own page after login.
 *
 * SecurityConfig points a successful login here, so this is the only place
 * that decides where someone lands. "/" is the public shop front and stays
 * that way — a signed-in user who opens it still sees the shop, not a
 * redirect loop.
 */
@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String home(Authentication authentication) {
        return "redirect:" + landingPageFor(authentication);
    }

    /** The admin console's own summary. Replaced by the real screens in M3. */
    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("title", "Admin");
        model.addAttribute("role", "ADMIN");
        model.addAttribute("items", java.util.List.of(
                "User accounts and roles",
                "Pricing and settings",
                "Stock",
                "Management reports",
                "Audit log"));
        return "landing";
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
