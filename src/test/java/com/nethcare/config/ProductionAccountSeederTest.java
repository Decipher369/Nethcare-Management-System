package com.nethcare.config;

import com.nethcare.model.Role;
import com.nethcare.model.User;
import com.nethcare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductionAccountSeederTest {
    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final MockEnvironment environment = new MockEnvironment();
    private final ProductionAccountSeeder seeder = new ProductionAccountSeeder(users, encoder, environment);

    @Test
    void createsOnlyAdminWithJustAdminPasswordConfigured() {
        String password = "TestAdminPassword123";
        environment.setProperty("seed.admin.password", password);
        seeder.run();

        var saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).existsByUsername("admin");
        verify(users).save(saved.capture());
        verifyNoMoreInteractions(users);
        User admin = saved.getValue();
        assertEquals("admin", admin.getUsername());
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(admin.isActive());
        assertTrue(admin.isMustChangePassword());
        assertTrue(encoder.matches(password, admin.getPasswordHash()));
    }

    @Test
    void preservesExistingAdminWithoutRequiringSeedPasswords() {
        when(users.existsByUsername("admin")).thenReturn(true);
        assertDoesNotThrow(() -> seeder.run());
        verify(users).existsByUsername("admin");
        verifyNoMoreInteractions(users);
    }

    @Test
    void rejectsMissingOrInvalidPasswordBeforeSaving() {
        for (String password : new String[]{"", "short123", "OnlyLettersHere", "123456789012", "a1" + "x".repeat(71)}) {
            environment.setProperty("seed.admin.password", password);
            assertThrows(IllegalStateException.class, () -> seeder.run());
        }
        verify(users, never()).save(any());
    }
}
