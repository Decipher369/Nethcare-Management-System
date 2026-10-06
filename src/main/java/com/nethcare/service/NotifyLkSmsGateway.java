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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "nethcare.sms.provider", havingValue = "notifylk")
public class NotifyLkSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(NotifyLkSmsGateway.class);

    private final String endpoint;
    private final String userId;
    private final String apiKey;
    private final String senderId;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public NotifyLkSmsGateway(
            @Value("${nethcare.sms.notifylk.endpoint:https://app.notify.lk/api/v1/send}") String endpoint,
            @Value("${nethcare.sms.notifylk.user-id:}") String userId,
            @Value("${nethcare.sms.notifylk.api-key:}") String apiKey,
            @Value("${nethcare.sms.notifylk.sender-id:NotifyDEMO}") String senderId,
            ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.userId = userId != null ? userId.trim() : "";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.senderId = senderId != null ? senderId.trim() : "NotifyDEMO";
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String send(String destination, String message) {
        if (userId.isBlank() || apiKey.isBlank()) {
            throw new BusinessException("Notify.lk credentials (user_id and api_key) are not configured.");
        }

        String normalizedRecipient = normalizePhoneForNotifyLk(destination);
        if (normalizedRecipient == null || normalizedRecipient.isBlank()) {
            throw new BusinessException("Invalid recipient phone number for Notify.lk: " + destination);
        }

        try {
            String formBody = "user_id=" + URLEncoder.encode(userId, StandardCharsets.UTF_8)
                    + "&api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&sender_id=" + URLEncoder.encode(senderId, StandardCharsets.UTF_8)
                    + "&to=" + URLEncoder.encode(normalizedRecipient, StandardCharsets.UTF_8)
                    + "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String responseBody = response.body();

            log.info("Notify.lk response status {}: {}", response.statusCode(), responseBody);

            if (response.statusCode() >= 200 && response.statusCode() < 300 && responseBody != null) {
                JsonNode root = objectMapper.readTree(responseBody);
                String status = root.path("status").asText("");
                if ("success".equalsIgnoreCase(status)) {
                    String receipt = "NLK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
                    log.info("Successfully dispatched SMS to {} via Notify.lk (receipt={})", normalizedRecipient, receipt);
                    return receipt;
                } else {
                    String errorMsg = root.has("errors") ? root.path("errors").toString() : responseBody;
                    throw new BusinessException("Notify.lk dispatch rejected: " + errorMsg);
                }
            } else {
                throw new BusinessException("Notify.lk HTTP error " + response.statusCode() + ": " + responseBody);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.error("Failed to send SMS to {} via Notify.lk: {}", normalizedRecipient, ex.getMessage(), ex);
            throw new BusinessException("Notify.lk dispatch failed: " + ex.getMessage());
        }
    }

    private String normalizePhoneForNotifyLk(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("0") && digits.length() == 10) {
            return "94" + digits.substring(1);
        }
        if (digits.startsWith("94") && digits.length() == 11) {
            return digits;
        }
        return digits;
    }

    @Override
    public String getProviderName() {
        return "Notify.lk (Sender: " + senderId + ")";
    }

    @Override
    public boolean isSimulator() {
        return false;
    }
}
