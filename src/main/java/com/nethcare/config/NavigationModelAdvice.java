package com.nethcare.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the signed-in identity available to shared navigation fragments. */
@ControllerAdvice
public class NavigationModelAdvice {

    @ModelAttribute("currentUser")
    public String currentUser(Authentication authentication) {
        return authentication == null ? "" : authentication.getName();
    }

    @ModelAttribute("currentRole")
    public String currentRole(Authentication authentication) {
        if (authentication == null) return "";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String name = authority.getAuthority();
            if (name.startsWith("ROLE_")) return name.substring(5);
        }
        return "";
    }
}
