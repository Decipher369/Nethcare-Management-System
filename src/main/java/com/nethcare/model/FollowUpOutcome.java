package com.nethcare.model;

/**
 * What the patient said when we asked them to come back.
 *
 * The client asked for the response to be recorded rather than just "done",
 * because no-show and a booking that was never made are different problems
 * for the front desk to chase.
 */
public enum FollowUpOutcome {

    BOOKED,
    DECLINED,
    NO_ANSWER,
    WRONG_NUMBER;

    public String label() {
        return switch (this) {
            case BOOKED -> "Booked";
            case DECLINED -> "Declined";
            case NO_ANSWER -> "No answer";
            case WRONG_NUMBER -> "Number invalid";
        };
    }
}
