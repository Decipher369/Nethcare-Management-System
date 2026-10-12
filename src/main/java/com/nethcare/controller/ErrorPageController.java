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
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int statusCode = 500;
        if (status instanceof Integer i && i != 200) {
            statusCode = i;
        } else if (status instanceof String s) {
            try {
                int parsed = Integer.parseInt(s);
                if (parsed != 200) statusCode = parsed;
            } catch (NumberFormatException ignored) {}
        }

        String msg = message != null && !message.toString().isBlank()
                ? message.toString()
                : (exception instanceof Throwable t ? t.getMessage() : "Something went wrong while processing your request.");

        model.addAttribute("status", statusCode);
        model.addAttribute("message", msg);
        model.addAttribute("path", path == null ? "" : path);

        return "error";
    }
}
