package com.nethcare.model;

/**
 * Where an order is in the pipeline.
 *
 *   PLACED -> LAB -> READY -> COLLECTED
 *
 * plus CANCELLED, which can happen from anywhere before collection. LAB is the
 * step the lab company takes; COLLECTED is the moment the customer walks out
 * with the glasses, and that is where stock is actually deducted.
 */
public enum OrderStatus {

    PLACED,
    LAB,
    READY,
    COLLECTED,
    CANCELLED;

    /** The next status this one is allowed to become. */
    public OrderStatus next() {
        return switch (this) {
            case PLACED -> LAB;
            case LAB -> READY;
            case READY -> COLLECTED;
            case COLLECTED, CANCELLED -> null;
        };
    }

    public boolean isOpen() {
        return this != COLLECTED && this != CANCELLED;
    }

    public boolean canBeCancelled() {
        return isOpen();
    }

    /** Shown on the order tracker so staff and patients read the same words. */
    public String label() {
        return switch (this) {
            case PLACED -> "Order placed";
            case LAB -> "At the lab";
            case READY -> "Ready for pickup";
            case COLLECTED -> "Collected";
            case CANCELLED -> "Cancelled";
        };
    }
}
