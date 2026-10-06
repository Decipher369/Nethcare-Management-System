package com.nethcare.service;

public interface SmsGateway {
    String send(String destination, String message);

    default String getProviderName() {
        return "Generic Gateway";
    }

    default boolean isSimulator() {
        return false;
    }
}

