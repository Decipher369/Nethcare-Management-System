package com.nethcare.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Placeholder pages each role lands on after login.
 *
 * They list what the person is allowed to do so the access rules can be
 * checked before the real screens exist. The real pages belong to their own
 * module: /patients is #24, /orders is #28, /referrals is #26.
 */
@Controller
public class LandingController {

    @GetMapping("/patients")
    public String patients(Model model) {
        return page(model, "Patient Registration", "OPTICIAN", List.of(
                "Register a new patient",
                "Search existing records",
                "Visit history timeline"));
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        return page(model, "Orders & Billing", "STAFF_NURSE", List.of(
                "Create an order from a prescription",
                "Printable bills and receipts",
                "Order status: placed, lab, ready, collected",
                "Stock levels and low-stock alerts"));
    }

    @GetMapping("/referrals")
    public String referrals(Model model) {
        return page(model, "Referrals", "SURGEON", List.of(
                "Patients referred to you",
                "Surgical notes"));
    }

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
