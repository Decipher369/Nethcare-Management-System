package com.nethcare.dto;

import com.nethcare.model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A staff account as the API hands it out.
 *
 * The password hash is not carried across at all — not hidden, not blanked,
 * simply never read off the entity. User has a getPasswordHash() and Jackson
 * would serialise it, so returning the entity directly put live BCrypt hashes
 * in the user list.
 */
public class UserDto {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String status;
    private LocalDateTime lastLoginAt;
    private boolean mustChangePassword;

    public static UserDto of(User u) {
        UserDto d = new UserDto();
        d.id = u.getId();
        d.username = u.getUsername();
        d.fullName = u.getFullName();
        d.email = u.getEmail();
        d.phone = u.getPhone();
        d.role = (u.getRole() == null) ? null : u.getRole().name();
        d.status = u.isActive() ? "ACTIVE" : "INACTIVE";
        d.lastLoginAt = u.getLastLoginAt();
        d.mustChangePassword = u.isMustChangePassword();
        return d;
    }

    public static List<UserDto> of(List<User> list) {
        return list.stream().map(UserDto::of).toList();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public boolean isMustChangePassword() { return mustChangePassword; }
}
