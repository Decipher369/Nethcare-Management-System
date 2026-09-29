package com.nethcare.service;

import com.nethcare.dto.TemporaryCredential;
import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.AuditAction;
import com.nethcare.model.Role;
import com.nethcare.model.User;
import com.nethcare.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserService(UserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    public List<User> list() { return users.findAllByOrderByUsernameAsc(); }

    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("No user with id " + id));
    }

    @Transactional
    public User create(String username, String rawPassword, String fullName, String email,
                       String phone, Role role, String actor) {
        username = normalizeUsername(username);
        if (role == null) throw new BusinessException("Role is required.");
        if (role == Role.PATIENT) throw new BusinessException("Patient accounts must be created from patient registration.");
        validatePassword(rawPassword);
        if (users.existsByUsername(username)) throw new BusinessException("Username " + username + " is already taken.");

        User user = new User(username, encoder.encode(rawPassword), role);
        user.setFullName(required(fullName, "Full name"));
        user.setEmail(normalizeEmail(email));
        user.setPhone(normalizePhone(phone));
        user.setIsActive(true);
        user.setMustChangePassword(true);
        User saved = users.save(user);
        audit.record(actor, AuditAction.CREATE, "User", saved.getId().toString(), null, summary(saved));
        return saved;
    }

    @Transactional
    public User changeRole(Long id, Role role, String actor) {
        if (role == null || role == Role.PATIENT) throw new BusinessException("Choose a valid staff role.");
        User user = get(id);
        if (user.getRole() == Role.PATIENT) throw new BusinessException("A patient account role cannot be changed here.");
        guardLastAdmin(user, role, true);
        Role before = user.getRole();
        user.setRole(role);
        User saved = users.save(user);
        audit.record(actor, AuditAction.UPDATE, "User", id.toString(), "role=" + before, "role=" + role);
        return saved;
    }

    @Transactional
    public User deactivate(Long id, String actor) {
        User user = get(id);
        if (user.getUsername().equalsIgnoreCase(actor)) throw new BusinessException("You cannot deactivate your own account.");
        guardLastAdmin(user, user.getRole(), false);
        user.setIsActive(false);
        User saved = users.save(user);
        audit.record(actor, AuditAction.DELETE, "User", id.toString(), "active", "inactive");
        return saved;
    }

    @Transactional
    public User reactivate(Long id, String actor) {
        User user = get(id);
        user.setIsActive(true);
        User saved = users.save(user);
        audit.record(actor, AuditAction.UPDATE, "User", id.toString(), "inactive", "active");
        return saved;
    }

    @Transactional
    public TemporaryCredential resetPassword(Long id, String actor) {
        User user = get(id);
        String password = randomPassword();
        user.setPasswordHash(encoder.encode(password));
        user.setMustChangePassword(true);
        users.save(user);
        audit.record(actor, AuditAction.UPDATE, "User", id.toString(), null, "password reset; change required");
        return new TemporaryCredential(user.getUsername(), password);
    }

    @Transactional
    public void changeOwnPassword(String username, String currentPassword, String newPassword) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        if (!encoder.matches(currentPassword, user.getPasswordHash())) throw new BusinessException("Current password is incorrect.");
        validatePassword(newPassword);
        if (encoder.matches(newPassword, user.getPasswordHash())) throw new BusinessException("New password must be different.");
        user.setPasswordHash(encoder.encode(newPassword));
        user.setMustChangePassword(false);
        users.save(user);
        audit.record(username, AuditAction.UPDATE, "User", user.getId().toString(), null, "password changed");
    }

    private void guardLastAdmin(User user, Role targetRole, boolean roleChange) {
        boolean removesAdmin = user.getRole() == Role.ADMIN && user.isActive()
                && (roleChange ? targetRole != Role.ADMIN : true);
        if (removesAdmin && users.countByRoleAndIsActiveTrue(Role.ADMIN) <= 1) {
            throw new BusinessException("The final active administrator cannot be removed.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 12 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new BusinessException("Password must have at least 12 characters, including a letter and a number.");
        }
    }

    private String randomPassword() {
        StringBuilder value = new StringBuilder("N3th!");
        for (int i = 0; i < 12; i++) value.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        return value.toString();
    }

    private String normalizeUsername(String value) {
        String username = required(value, "Username").toLowerCase(Locale.ROOT);
        if (!username.matches("[a-z0-9._-]{3,50}")) throw new BusinessException("Username must be 3-50 letters, numbers, dots, underscores or hyphens.");
        return username;
    }

    private String normalizeEmail(String value) {
        String email = trim(value);
        if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new BusinessException("Email address is invalid.");
        return email == null ? null : email.toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String value) {
        String phone = trim(value);
        if (phone == null) return null;
        phone = phone.replaceAll("[\\s()-]", "");
        if (phone.matches("0\\d{9}")) phone = "+94" + phone.substring(1);
        else if (phone.matches("94\\d{9}")) phone = "+" + phone;
        if (!phone.matches("\\+94\\d{9}")) throw new BusinessException("Phone must be a valid Sri Lankan phone number.");
        return phone;
    }

    private String required(String value, String label) {
        String v = trim(value);
        if (v == null) throw new BusinessException(label + " is required.");
        return v;
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String summary(User user) { return "username=" + user.getUsername() + ", role=" + user.getRole() + ", active=" + user.isActive(); }
}
