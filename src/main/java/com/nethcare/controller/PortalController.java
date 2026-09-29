package com.nethcare.controller;

import com.nethcare.dto.ApiResponse;
import com.nethcare.dto.PortalData;
import com.nethcare.service.PortalService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PortalController {
    private final PortalService portal;
    public PortalController(PortalService portal) { this.portal = portal; }

    @GetMapping("/portal")
    public String home(Authentication authentication, Model model) {
        PortalData data = portal.forUser(authentication.getName());
        model.addAttribute("patient", data.patient());
        model.addAttribute("prescriptions", data.prescriptions());
        model.addAttribute("orders", data.orders());
        return "portal/home";
    }

    @GetMapping("/api/portal")
    @ResponseBody
    public ApiResponse<PortalData> data(Authentication authentication) {
        return ApiResponse.success(portal.forUser(authentication.getName()));
    }
}
