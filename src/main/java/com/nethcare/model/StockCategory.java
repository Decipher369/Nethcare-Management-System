package com.nethcare.model;

/**
 * The five columns the stock dashboard shows, in the order the client asked
 * for them: Frames, SV lens, Bifocal, Contacts, Cases.
 */
public enum StockCategory {

    FRAME,
    SINGLE_VISION_LENS,
    BIFOCAL_LENS,
    CONTACT_LENS,
    CASE;

    /** Heading used on the dashboard and the public gallery filter. */
    public String label() {
        return switch (this) {
            case FRAME -> "Frames";
            case SINGLE_VISION_LENS -> "SV lens";
            case BIFOCAL_LENS -> "Bifocal";
            case CONTACT_LENS -> "Contacts";
            case CASE -> "Cases";
        };
    }
}
