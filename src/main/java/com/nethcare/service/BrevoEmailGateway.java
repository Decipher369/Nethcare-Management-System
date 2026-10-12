package com.nethcare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nethcare.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "nethcare.email.provider", havingValue = "brevo")
public class BrevoEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailGateway.class);

    private final String endpoint;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BrevoEmailGateway(
            @Value("${nethcare.email.brevo.endpoint:https://api.brevo.com/v3/smtp/email}") String endpoint,
            @Value("${nethcare.email.brevo.api-key:}") String apiKey,
            @Value("${nethcare.email.brevo.sender-email:no-reply@nethcare.lk}") String senderEmail,
            @Value("${nethcare.email.brevo.sender-name:Nethcare Clinic}") String senderName,
            ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.senderEmail = senderEmail != null ? senderEmail.trim() : "";
        this.senderName = senderName != null ? senderName.trim() : "Nethcare Clinic";
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String send(String destination, String subject, String message) {
        if (apiKey.isBlank()) {
            throw new BusinessException("Brevo API key is not configured.");
        }
        if (destination == null || destination.isBlank()) {
            throw new BusinessException("Recipient email address is required.");
        }

        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", List.of(Map.of("email", destination.trim())),
                    "subject", (subject != null && !subject.isBlank()) ? subject.trim() : "Nethcare Notification",
                    "textContent", message != null ? message : "",
                    "htmlContent", "<div style=\"font-family:sans-serif;line-height:1.6;color:#1e293b;padding:20px;\">"
                            + "<h2 style=\"color:#1e3a8a;\">" + ((subject != null && !subject.isBlank()) ? subject : "Nethcare Notification") + "</h2>"
                            + "<p>" + (message != null ? message.replace("\n", "<br>") : "") + "</p>"
                            + "<hr style=\"border:none;border-top:1px solid #e2e8f0;margin:20px 0;\">"
                            + "<p style=\"font-size:12px;color:#64748b;\">Nethcare Management System · Kolonnawa</p>"
                            + "</div>"
            );

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                String messageId = "BREVO-OK";
                try {
                    JsonNode node = objectMapper.readTree(responseBody);
                    if (node.has("messageId")) {
                        messageId = node.get("messageId").asText();
                    }
                } catch (Exception ignore) {}
                log.info("Email dispatched successfully to {} via Brevo (messageId={})", destination, messageId);
                return messageId;
            } else {
                String errorMsg = "HTTP " + statusCode;
                try {
                    JsonNode node = objectMapper.readTree(responseBody);
                    if (node.has("message")) {
                        errorMsg = node.get("message").asText();
                    }
                } catch (Exception ignore) {}
                log.error("Brevo API error (status={}): {}", statusCode, responseBody);
                throw new BusinessException("Brevo dispatch failed: " + errorMsg);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.error("Failed to send email to {} via Brevo: {}", destination, ex.getMessage(), ex);
            throw new BusinessException("Brevo error: " + ex.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "Brevo API";
    }

    @Override
    public boolean isSimulator() {
        return false;
    }
}
