package com.nethcare.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Spring Security needs a page at /login to post the form to.
 *
 * Plain for now, the styled version is issue #2.
 */
@Controller
public class LoginController {

    // Only the dev profile sets this, so the dev passwords stay hidden elsewhere
    private final boolean showHint;

    public LoginController(@Value("${seed.show-hint:false}") boolean showHint) {
        this.showHint = showHint;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("showHint", showHint);
        return "login";
    }
}
