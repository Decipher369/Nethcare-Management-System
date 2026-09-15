package com.nethcare.model;

import java.util.List;

/**
 * The shop's public-facing details, in one place.
 *
 * The client proposal and the deck only ever gave us the business name, the
 * town, the owner's name and the fact that it is an optometrist and glasses
 * shop. Everything else here comes from those documents, and anything we were
 * not told is left as null so the page says "to confirm" rather than printing
 * an invented phone number on a real business.
 *
 * These are constants rather than database rows because the client has not
 * asked for them to be editable. If that changes, this becomes a table and
 * the admin console gains a settings screen — the templates already read
 * every value from here rather than hard-coding text.
 */
public final class BusinessProfile {

    private BusinessProfile() {
    }

    public static final String NAME = "Neth Opticians";
    public static final String LOCATION = "Kolonnawa, Sri Lanka";
    public static final String OWNER = "Ms. Udeni Gurusinghe";

    /** One line for the top of the page. */
    public static final String TAGLINE =
            "Optometrist and prescription eyewear, measured on site and fitted by hand.";

    /** What the premises actually contain — the proposal lists these three. */
    public static final String PREMISES = "Reception, examination room, dispensing counter";

    /**
     * Not in any client document. Shown as "to confirm" on the page until the
     * client gives us the real details — a made-up phone number on a real shop
     * is worse than a blank.
     */
    public static final String PHONE = null;
    public static final String EMAIL = null;
    public static final String ADDRESS_LINE = null;
    public static final String OPENING_HOURS = null;

    /**
     * What the shop does, taken from the project README and the proposal.
     * Not a marketing list — each line is a service the system actually
     * supports.
     */
    public static List<Service> services() {
        return List.of(
                new Service("Eye examinations",
                        "A full refraction test in the examination room, with the result kept "
                                + "on the patient's record."),
                new Service("Prescription eyewear",
                        "Single vision, bifocal and progressive lenses, cut to the prescription "
                                + "taken on the day."),
                new Service("Frames",
                        "A stocked range, from everyday pairs to metal and acetate designs."),
                new Service("Contact lenses",
                        "Fitted and supplied with the expiry tracked, so nothing past its date "
                                + "stays on the shelf."),
                new Service("Repairs and adjustments",
                        "Frame repairs, hinge and temple work, and free adjustments for glasses "
                                + "bought here."),
                new Service("Glasses for the family",
                        "Repeat orders kept on file, so a replacement pair can be matched to the "
                                + "last one without a re-examination."));
    }

    /** One line of the services list. */
    public record Service(String title, String detail) {
    }

    /** True when a detail has not been supplied by the client yet. */
    public static boolean isPending(String value) {
        return value == null || value.isBlank();
    }
}
