package com.nethcare.controller;

import com.nethcare.exception.BusinessException;
import com.nethcare.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {
    private final UserService users;

    public AccountController(UserService users) { this.users = users; }

    @GetMapping("/account/change-password")
    public String form() { return "account/change-password"; }

    @PostMapping("/account/change-password")
    public String change(@RequestParam String currentPassword, @RequestParam String newPassword,
                         @RequestParam String confirmPassword, Authentication authentication,
                         Model model, RedirectAttributes redirect) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New passwords do not match.");
            return "account/change-password";
        }
        try {
            users.changeOwnPassword(authentication.getName(), currentPassword, newPassword);
            redirect.addFlashAttribute("success", "Password changed successfully.");
            return "redirect:/dashboard";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "account/change-password";
        }
    }
}
