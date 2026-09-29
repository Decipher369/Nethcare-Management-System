package com.nethcare.service;

import com.nethcare.model.LoginEvent;
import com.nethcare.repository.LoginEventRepository;
import com.nethcare.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthenticationEventService {
    private final UserRepository users;
    private final LoginEventRepository events;

    public AuthenticationEventService(UserRepository users, LoginEventRepository events) {
        this.users = users;
        this.events = events;
    }

    @Transactional
    public boolean success(Authentication authentication, HttpServletRequest request) {
        LoginEvent event = base(authentication.getName(), request);
        event.setSuccessful(true);
        final boolean[] mustChange = {false};
        users.findByUsername(authentication.getName()).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            users.save(user);
            event.setUserId(user.getId());
            mustChange[0] = user.isMustChangePassword();
        });
        events.save(event);
        return mustChange[0];
    }

    @Transactional
    public void failure(String username, String reason, HttpServletRequest request) {
        String safeUsername = username == null || username.isBlank() ? "(blank)" : username.trim();
        LoginEvent event = base(safeUsername.substring(0, Math.min(50, safeUsername.length())), request);
        event.setSuccessful(false);
        event.setFailureReason(reason == null ? "Authentication failed" : reason.substring(0, Math.min(100, reason.length())));
        users.findByUsername(safeUsername).ifPresent(user -> event.setUserId(user.getId()));
        events.save(event);
    }

    private LoginEvent base(String username, HttpServletRequest request) {
        LoginEvent event = new LoginEvent();
        event.setUsername(username);
        event.setOccurredAt(LocalDateTime.now());
        event.setIpAddress(clientIp(request));
        String agent = request.getHeader("User-Agent");
        event.setUserAgent(agent == null ? null : agent.substring(0, Math.min(300, agent.length())));
        return event;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return ip.substring(0, Math.min(45, ip.length()));
    }
}
