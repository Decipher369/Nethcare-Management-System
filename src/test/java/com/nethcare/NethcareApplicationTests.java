package com.nethcare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test — verifies the Spring context loads without errors.
 */
@SpringBootTest
@ActiveProfiles("test")
class NethcareApplicationTests {

    @Test
    void contextLoads() {
        // If the context fails to load, this test will fail
        assertTrue(true, "Spring context loaded successfully");
    }
}
