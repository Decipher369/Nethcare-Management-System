package com.nethcare.model;

/**
 * Whether the customer is in a hurry, and what that costs.
 *
 * The client asked for normal and urgent ordering with a surcharge (FR-3.3).
 * The percentage lives here rather than in the order so the rule has one home.
 */
public enum OrderPriority {

    NORMAL(0),
    URGENT(15);

    private final int surchargePercent;

    OrderPriority(int surchargePercent) {
        this.surchargePercent = surchargePercent;
    }

    public int surchargePercent() {
        return surchargePercent;
    }
}
