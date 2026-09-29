package com.nethcare.model;

/**
 * Where a follow-up is in the process.
 *
 * ACTIVE cases have not been queued. REVIEW_QUEUED and NOTIFIED retain the
 * communication state; ATTENDED and DEFAULTED close the clinical workflow.
 */
public enum FollowUpStatus {

    ACTIVE,
    REVIEW_QUEUED,
    NOTIFIED,
    ATTENDED,
    DEFAULTED;

    public boolean isOpen() {
        return this == ACTIVE || this == REVIEW_QUEUED || this == NOTIFIED;
    }

    public String label() {
        return switch (this) {
            case ACTIVE -> "Active";
            case REVIEW_QUEUED -> "Review queued";
            case NOTIFIED -> "Notified";
            case ATTENDED -> "Attended";
            case DEFAULTED -> "Defaulted";
        };
    }
}
