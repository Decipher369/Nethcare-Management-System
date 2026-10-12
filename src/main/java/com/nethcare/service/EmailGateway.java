package com.nethcare.service;

public interface EmailGateway {
    String send(String destination, String subject, String message);

    default String send(String destination, String message) {
        return send(destination, "Nethcare Notification", message);
    }

    default String getProviderName() {
        return "Generic Email Gateway";
    }

    default boolean isSimulator() {
        return false;
    }
}
