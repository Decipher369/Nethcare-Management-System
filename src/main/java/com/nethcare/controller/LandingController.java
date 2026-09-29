package com.nethcare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Placeholder pages each role lands on after login.
 *
 * They list what the person is allowed to do so the access rules can be
 * checked before the real screens exist. Patient registration and orders now
 * have their own screens; only the patient portal remains a placeholder.
 */
@Controller
public class LandingController {

    @GetMapping("/portal")
    public String portal(Model model) {
        return page(model, "My Records", "PATIENT", List.of(
                "My profile",
                "My prescriptions",
                "My order status"));
    }

    private String page(Model model, String title, String role, List<String> items) {
        model.addAttribute("title", title);
        model.addAttribute("role", role);
        model.addAttribute("items", items);
        return "landing";
    }
}
