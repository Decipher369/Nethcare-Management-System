package com.nethcare.service;

public interface SmsGateway {
    String send(String destination, String message);
}
