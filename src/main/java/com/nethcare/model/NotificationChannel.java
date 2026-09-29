package com.nethcare.model;

/**
 * How a reminder reaches the patient.
 *
 * SMS is tried first and email only if the SMS fails, which is the order the
 * client asked for.
 */
public enum NotificationChannel {

    SMS,
    EMAIL;

    public String label() {
        return this == SMS ? "SMS" : "Email";
    }
}
