package com.nethcare.model;

/**
 * How money reached the counter.
 *
 * Card and cash only, recorded by staff. Nethcare has no payment gateway and
 * no card details are stored — this records that a payment happened, not how
 * the money moved.
 */
public enum PaymentMethod {

    CASH,
    CARD;

    public String label() {
        return this == CASH ? "Cash" : "Card";
    }
}
