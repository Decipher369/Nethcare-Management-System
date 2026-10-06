package com.nethcare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nethcare.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotifyLkSmsGateway Unit Tests")
class NotifyLkSmsGatewayTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Provider name and isSimulator metadata are correct")
    void providerMetadata() {
        NotifyLkSmsGateway gateway = new NotifyLkSmsGateway(
                "https://app.notify.lk/api/v1/send",
                "12345",
                "mock-api-key",
                "NotifyDEMO",
                objectMapper
        );

        assertFalse(gateway.isSimulator());
        assertEquals("Notify.lk (Sender: NotifyDEMO)", gateway.getProviderName());
    }

    @Test
    @DisplayName("Throws exception if user ID or API key are missing")
    void throwsWhenCredentialsMissing() {
        NotifyLkSmsGateway gateway = new NotifyLkSmsGateway(
                "https://app.notify.lk/api/v1/send",
                "",
                "",
                "NotifyDEMO",
                objectMapper
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                gateway.send("0771234567", "Hello test"));

        assertTrue(ex.getMessage().contains("credentials"));
    }
}
