package com.nethcare.model;

/**
 * How a reminder reaches the patient.
 *
 * SMS is tried first and email only if the SMS fails, which is the order the
 * client asked for.
 */
public enum NotificationChannel {

    SMS,
    IN_APP_PORTAL,
    BOTH;

    public String label() {
        return switch (this) {
            case SMS -> "SMS";
            case IN_APP_PORTAL -> "In-app portal";
            case BOTH -> "SMS and in-app portal";
        };
    }
}
