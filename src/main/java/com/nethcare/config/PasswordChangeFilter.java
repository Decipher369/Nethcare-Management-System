package com.nethcare.config;

import com.nethcare.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class PasswordChangeFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public PasswordChangeFilter(UserRepository users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();
        boolean allowed = path.startsWith("/account/change-password") || path.equals("/logout")
                || path.equals("/login") || path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.equals("/error");
        if (!allowed && auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && users.findByUsername(auth.getName()).map(user -> user.isMustChangePassword()).orElse(false)) {
            if (path.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Change the temporary password before continuing.\"}");
            } else response.sendRedirect("/account/change-password");
            return;
        }
        chain.doFilter(request, response);
    }
}
