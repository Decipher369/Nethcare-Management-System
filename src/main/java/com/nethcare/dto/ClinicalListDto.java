package com.nethcare.dto;

import com.nethcare.model.Prescription;
import com.nethcare.model.Referral;

import java.time.LocalDate;

/**
 * The two list views a clinician works from, with the rule that matters
 * attached to each row.
 *
 * Validity and access expiry live on the entity as isValidOn / isOpenForAccess
 * and were never called from anywhere — a client had to work them out for
 * itself. Both are computed here so the API states the answer instead of
 * leaving the rule to whoever is reading.
 */
public class ClinicalListDto {

    Long id;
    String number;
    Long patientId;
    LocalDate date;
    String summary;
    Boolean valid;
    LocalDate validUntil;
    Boolean accessible;
    String status;

    public static PrescriptionDto ofPrescription(Prescription rx, LocalDate today) {
        return new PrescriptionDto(rx, today);
    }

    public static ReferralDto ofReferral(Referral r, LocalDate today) {
        return new ReferralDto(r, today);
    }

    /** A prescription row: the powers, plus whether it can still be ordered. */
    public static class PrescriptionDto extends ClinicalListDto {

        private String od;
        private String os;

        PrescriptionDto(Prescription rx, LocalDate today) {
            this.id = rx.getId();
            this.number = rx.getRxNo();
            this.patientId = rx.getPatientId();
            this.date = rx.getIssuedOn();
            this.validUntil = rx.getExpiresOn();
            this.valid = rx.isValidOn(today);
            this.od = power(rx.getOdSph(), rx.getOdCyl(), rx.getOdAxis(), rx.getOdAdd());
            this.os = power(rx.getOsSph(), rx.getOsCyl(), rx.getOsAxis(), rx.getOsAdd());
            this.summary = this.valid
                    ? "Valid until " + this.validUntil
                    : "Expired on " + this.validUntil + " — a new examination is needed before ordering";
        }

        public String getOd() { return od; }
        public String getOs() { return os; }
    }

    /** A referral row: who it is for, how urgent, and whether it is still open. */
    public static class ReferralDto extends ClinicalListDto {

        private String surgeonName;
        private String reason;

        ReferralDto(Referral r, LocalDate today) {
            this.id = r.getId();
            this.number = r.getRefNo();
            this.patientId = r.getPatientId();
            this.date = r.getReferredOn();
            this.surgeonName = r.getSurgeonName();
            this.reason = r.getReason();
            this.status = r.getStatus();
            this.accessible = r.isOpenForAccess(today);
            this.validUntil = r.getReferredOn().plusDays(Referral.ACCESS_DAYS);
            this.summary = this.accessible
                    ? "Open until " + this.validUntil
                    : "Access closed on " + this.validUntil;
        }

        public String getSurgeonName() { return surgeonName; }
        public String getReason() { return reason; }
    }

    // Base curve and IPD are not part of the sphere/cylinder/axis run, so they
    // are added on rather than folded in.
    private static String power(String sph, String cyl, String axis, String add) {
        StringBuilder sb = new StringBuilder();
        if (isSet(sph)) { sb.append(sph); }
        if (isSet(cyl)) { sb.append(' ').append(cyl); }
        if (isSet(axis)) { sb.append(" x ").append(axis); }
        if (isSet(add)) { sb.append("  add ").append(add); }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    private static boolean isSet(String s) {
        return s != null && !s.isBlank();
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public Long getPatientId() { return patientId; }
    public LocalDate getDate() { return date; }
    public String getSummary() { return summary; }
    public Boolean getValid() { return valid; }
    public LocalDate getValidUntil() { return validUntil; }
    public Boolean getAccessible() { return accessible; }
    public String getStatus() { return status; }
}
