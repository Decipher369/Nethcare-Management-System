package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.Role;
import com.nethcare.model.User;
import com.nethcare.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Staff accounts — the admin-only half of M1.
 *
 * Separate from PatientService because these are logins for people who work
 * here, not records of people who come in. A patient is never a row in users.
 */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public List<User> list() {
        return users.findAllByOrderByUsernameAsc();
    }

    public User get(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + id));
    }

    @Transactional
    public User create(String username, String rawPassword, String fullName, String email, Role role) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("Username is required.");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new BusinessException("Password must be at least 6 characters.");
        }
        if (role == null) {
            throw new BusinessException("Role is required.");
        }
        if (users.findByUsername(username.trim()).isPresent()) {
            throw new BusinessException("Username " + username.trim() + " is already taken.");
        }

        User u = new User(username.trim(), encoder.encode(rawPassword), role);
        u.setFullName(trimOrNull(fullName));
        u.setEmail(trimOrNull(email));
        u.setStatus("ACTIVE");
        return users.save(u);
    }

    @Transactional
    public User changeRole(Long id, Role role) {
        if (role == null) {
            throw new BusinessException("Role is required.");
        }
        User u = get(id);
        u.setRole(role);
        return users.save(u);
    }

    // Deactivating flips status and is_active; nothing is deleted. Registrations
    // and other records point at user_id, and losing the row would orphan them.
    @Transactional
    public User deactivate(Long id) {
        User u = get(id);
        u.setStatus("INACTIVE");
        u.setIsActive(false);
        return users.save(u);
    }

    private String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
