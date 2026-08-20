package com.nethcare.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Catches anything Spring forwards here after an error.
 *
 * Without this, BasicErrorController gets no error details and replies with
 * "status=999, error=None", which then renders as the whitelabel page. Reading
 * the attributes off the request gives a real message instead.
 */
@Controller
public class ErrorPageController implements ErrorController {

    @RequestMapping("/error")
    public String error(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        model.addAttribute("status", status == null ? 500 : status);
        model.addAttribute("message", message == null ? "Something went wrong" : message);
        model.addAttribute("path", path == null ? "" : path);

        return "error";
    }
}
