package com.nethcare.model;

/**
 * Where a follow-up is in the process.
 *
 * PENDING means nobody has been reached yet. Once the patient answers it
 * becomes either BOOKED or DECLINED, and CLOSED is for the ones that came and
 * went without booking — those stay on the report as a response rate.
 */
public enum FollowUpStatus {

    PENDING,
    BOOKED,
    DECLINED,
    CLOSED;

    public boolean isOpen() {
        return this == PENDING;
    }

    public String label() {
        return switch (this) {
            case PENDING -> "Waiting";
            case BOOKED -> "Booked";
            case DECLINED -> "Declined";
            case CLOSED -> "Closed";
        };
    }
}
