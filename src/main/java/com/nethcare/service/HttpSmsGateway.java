package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "nethcare.sms.provider", havingValue = "http")
public class HttpSmsGateway implements SmsGateway {
    private final RestClient client;
    private final String endpoint;

    public HttpSmsGateway(RestClient.Builder builder,
                          @Value("${nethcare.sms.endpoint}") String endpoint,
                          @Value("${nethcare.sms.token}") String token) {
        this.client = builder.defaultHeader("Authorization", "Bearer " + token).build();
        this.endpoint = endpoint;
    }

    @Override
    public String send(String destination, String message) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = client.post().uri(endpoint)
                .body(Map.of("to", destination, "message", message))
                .retrieve().body(Map.class);
        Object receipt = response == null ? null : response.get("receiptId");
        if (receipt == null || receipt.toString().isBlank()) {
            throw new BusinessException("SMS gateway returned no receipt id.");
        }
        return receipt.toString();
    }

    @Override
    public String getProviderName() {
        return "HTTP Gateway (" + endpoint + ")";
    }
}

