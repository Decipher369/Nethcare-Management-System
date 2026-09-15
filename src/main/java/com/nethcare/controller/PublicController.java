package com.nethcare.controller;

import com.nethcare.model.BusinessProfile;
import com.nethcare.model.StockCategory;
import com.nethcare.service.StockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * The public shop front — the part a customer sees without an account.
 *
 * Three pages: who we are, what we do, and the frames we stock. All of them
 * read from the catalogue, so the gallery is not a hand-kept list that drifts
 * out of date — an item the counter retires stops appearing on its own.
 *
 * The gallery deliberately shows "available" and never the exact count. Saying
 * "2 left" on a public page is a countdown for somebody else's shop to beat
 * us to; the staff screen is where the numbers live.
 */
@Controller
public class PublicController {

    private final StockService stock;

    public PublicController(StockService stock) {
        this.stock = stock;
    }

    /** Business details and services. */
    @GetMapping("/")
    public String index(Model model) {
        profile(model);
        model.addAttribute("active", "home");
        return "public/home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        profile(model);
        model.addAttribute("active", "about");
        return "public/about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        profile(model);
        model.addAttribute("active", "contact");
        return "public/contact";
    }

    /** The frame gallery, with an optional category filter. */
    @GetMapping("/frames")
    public String frames(@RequestParam(required = false) StockCategory category,
                        @RequestParam(required = false) String q,
                        Model model) {
        profile(model);
        model.addAttribute("active", "frames");
        model.addAttribute("selected", category);
        model.addAttribute("query", q);
        model.addAttribute("items", stock.gallery(category, q));
        model.addAttribute("categories", List.of(StockCategory.values()));
        return "public/frames";
    }

    private void profile(Model model) {
        model.addAttribute("biz", BusinessProfile.class);
    }
}
