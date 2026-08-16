package com.nethcare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Page controller for serving Thymeleaf UI templates.
 * Handles the basic landing page and module navigation.
 */
@Controller
public class PageController {

    /**
     * Landing page — shows the Nethcare home screen with module navigation.
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", "Nethcare Management System");
        model.addAttribute("clientName", "Neth Opticians, Kolonnawa");
        return "index";
    }
}
