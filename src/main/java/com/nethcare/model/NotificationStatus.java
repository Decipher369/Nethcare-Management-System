package com.nethcare.model;

/**
 * Where a queued reminder has got to.
 *
 * SENT means a staff member recorded handing it over to the gateway — the
 * shop has no SMS provider wired up, so the app cannot claim a message was
 * delivered on its own. FAILED keeps the reason, because "invalid number" and
 * "gateway down" need different handling at the counter.
 */
public enum NotificationStatus {

    QUEUED,
    SENT,
    FAILED,
    SKIPPED;

    public String label() {
        return switch (this) {
            case QUEUED -> "Queued";
            case SENT -> "Sent";
            case FAILED -> "Failed";
            case SKIPPED -> "Skipped";
        };
    }
}
