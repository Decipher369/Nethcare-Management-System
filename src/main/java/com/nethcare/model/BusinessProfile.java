package com.nethcare.model;

import java.util.List;

/**
 * The shop's public-facing details, in one place.
 *
 * The client proposal and deck supply the shop details. The Siri Suwasetha
 * eyecare poster supplies the service location, phone number and service list.
 * Details absent from both sources stay null until confirmed.
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
    public static final String SERVICE_LOCATION = "Siri Suwasetha Medical Centre";
    public static final String SERVICE_AFFILIATION = "YIMBA - Kolonnawa";
    public static final String SERVICE_SINCE = "2001";

    /** One line for the top of the page. */
    public static final String TAGLINE =
            "Optometrist and prescription eyewear, measured on site and fitted by hand.";

    /** What the premises actually contain — the proposal lists these three. */
    public static final String PREMISES = "Reception, examination room, dispensing counter";

    /** Contact details printed on the eyecare service poster. */
    public static final String PHONE = "+94 112 532 153";
    public static final String PHONE_URI = "+94112532153";
    public static final String EMAIL = null;
    public static final String ADDRESS_LINE = "No. 470, Kolonnawa Road, Kolonnawa";
    public static final String OPENING_HOURS = null;

    /** Services listed on the Siri Suwasetha eyecare poster. */
    public static List<Service> services() {
        return List.of(
                new Service("Specialist ophthalmology consultation",
                        "Consultation for eye health concerns."),
                new Service("Comprehensive eye care",
                        "Personal attention to vision and eye health."),
                new Service("Glaucoma screening",
                        "Screening for glaucoma as part of the eyecare service."),
                new Service("Refraction & optical service",
                        "Vision testing and prescription eyewear."),
                new Service("Quality and designer spectacle frames & accessories",
                        "Frames and accessories with help finding a comfortable fit."));
    }

    /** One line of the services list. */
    public record Service(String title, String detail) {
    }

    /** True when a detail has not been supplied by the client yet. */
    public static boolean isPending(String value) {
        return value == null || value.isBlank();
    }
}
