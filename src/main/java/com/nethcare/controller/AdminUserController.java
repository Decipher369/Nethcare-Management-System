package com.nethcare.controller;

import com.nethcare.dto.TemporaryCredential;
import com.nethcare.dto.UserForm;
import com.nethcare.exception.BusinessException;
import com.nethcare.model.Role;
import com.nethcare.repository.LoginEventRepository;
import com.nethcare.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserService users;
    private final LoginEventRepository loginEvents;
    public AdminUserController(UserService users, LoginEventRepository loginEvents) {
        this.users = users; this.loginEvents = loginEvents;
    }

    @GetMapping
    public String list(Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", new UserForm());
        model.addAttribute("users", users.list());
        model.addAttribute("roles", java.util.Arrays.stream(Role.values()).filter(r -> r != Role.PATIENT).toList());
        model.addAttribute("loginEvents", loginEvents.findTop200ByOrderByOccurredAtDesc());
        return "admin/users";
    }

    @PostMapping
    public String create(@ModelAttribute UserForm form, Authentication auth, RedirectAttributes redirect) {
        try {
            users.create(form.getUsername(), form.getPassword(), form.getFullName(), form.getEmail(),
                    form.getPhone(), form.getRole(), auth.getName());
            redirect.addFlashAttribute("success", "Staff account created. The user must change the password at first login.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); redirect.addFlashAttribute("form", form); }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/role")
    public String role(@PathVariable Long id, @RequestParam Role role, Authentication auth, RedirectAttributes redirect) {
        try { users.changeRole(id, role, auth.getName()); redirect.addFlashAttribute("success", "Role updated."); }
        catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try { users.deactivate(id, auth.getName()); redirect.addFlashAttribute("success", "Account deactivated."); }
        catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        users.reactivate(id, auth.getName()); redirect.addFlashAttribute("success", "Account reactivated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reset-password")
    public String reset(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        TemporaryCredential credential = users.resetPassword(id, auth.getName());
        redirect.addFlashAttribute("credential", credential);
        return "redirect:/admin/users";
    }
}
