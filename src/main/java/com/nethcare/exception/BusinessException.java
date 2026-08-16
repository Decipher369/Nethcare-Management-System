package com.nethcare.exception;

/**
 * Thrown when a business rule is violated.
 * Examples: duplicate NIC, prescription expired, advance below 40%.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
