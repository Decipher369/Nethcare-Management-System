package com.nethcare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nethcare.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrevoEmailGatewayTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void throwsExceptionWhenApiKeyBlank() {
        BrevoEmailGateway gateway = new BrevoEmailGateway(
                "https://api.brevo.com/v3/smtp/email",
                "",
                "sender@example.com",
                "Nethcare",
                objectMapper
        );

        assertThrows(BusinessException.class, () ->
                gateway.send("patient@example.com", "Subject", "Hello")
        );
    }

    @Test
    void throwsExceptionWhenDestinationBlank() {
        BrevoEmailGateway gateway = new BrevoEmailGateway(
                "https://api.brevo.com/v3/smtp/email",
                "test-api-key",
                "sender@example.com",
                "Nethcare",
                objectMapper
        );

        assertThrows(BusinessException.class, () ->
                gateway.send("", "Subject", "Hello")
        );
    }

    @Test
    void reportsLiveProviderNameAndNotSimulator() {
        BrevoEmailGateway gateway = new BrevoEmailGateway(
                "https://api.brevo.com/v3/smtp/email",
                "test-api-key",
                "sender@example.com",
                "Nethcare",
                objectMapper
        );

        assertFalse(gateway.isSimulator());
        assertEquals("Brevo API", gateway.getProviderName());
    }
}
