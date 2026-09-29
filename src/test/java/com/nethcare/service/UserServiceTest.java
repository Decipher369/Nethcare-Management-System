package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.model.Role;
import com.nethcare.model.User;
import com.nethcare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock AuditService audit;
    UserService service;

    @BeforeEach void setUp() { service = new UserService(users, encoder, audit); }

    @Test
    void cannotDeactivateOwnAccount() {
        User admin = user(1L, "admin", Role.ADMIN);
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        assertThrows(BusinessException.class, () -> service.deactivate(1L, "admin"));
        verify(users, never()).save(any());
    }

    @Test
    void cannotRemoveFinalActiveAdministrator() {
        User admin = user(1L, "admin", Role.ADMIN);
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        when(users.countByRoleAndIsActiveTrue(Role.ADMIN)).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.changeRole(1L, Role.OPTICIAN, "other-admin"));
        verify(users, never()).save(any());
    }

    private User user(Long id, String username, Role role) {
        User user = new User(username, "hash", role); user.setId(id); user.setIsActive(true); return user;
    }
}
