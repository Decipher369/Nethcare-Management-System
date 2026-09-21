package com.nethcare.dto;

import com.nethcare.model.Prescription;
import com.nethcare.model.Referral;

import java.time.LocalDate;

/**
 * Compact rows used by the permanent prescription and referral histories.
 */
public class ClinicalListDto {

    Long id;
    String number;
    Long patientId;
    LocalDate date;
    String summary;
    String status;

    public static PrescriptionDto ofPrescription(Prescription rx, LocalDate ignored) {
        return new PrescriptionDto(rx);
    }

    public static ReferralDto ofReferral(Referral r, LocalDate ignored) {
        return new ReferralDto(r);
    }

    /** A prescription row: the powers, plus whether it can still be ordered. */
    public static class PrescriptionDto extends ClinicalListDto {

        private String od;
        private String os;

        PrescriptionDto(Prescription rx) {
            this.id = rx.getId();
            this.number = rx.getRxNo();
            this.patientId = rx.getPatientId();
            this.date = rx.getIssuedOn();
            this.od = power(rx.getOdSph(), rx.getOdCyl(), rx.getOdAxis(), rx.getOdAdd());
            this.os = power(rx.getOsSph(), rx.getOsCyl(), rx.getOsAxis(), rx.getOsAdd());
            this.summary = "Permanent clinical record";
        }

        public String getOd() { return od; }
        public String getOs() { return os; }
    }

    /** A referral row: who it is for, how urgent, and whether it is still open. */
    public static class ReferralDto extends ClinicalListDto {

        private String surgeonName;
        private String reason;

        ReferralDto(Referral r) {
            this.id = r.getId();
            this.number = r.getRefNo();
            this.patientId = r.getPatientId();
            this.date = r.getReferredOn();
            this.surgeonName = r.getSurgeonName();
            this.reason = r.getReason();
            this.status = r.getStatus();
            this.summary = "PENDING".equalsIgnoreCase(r.getStatus())
                    ? "Awaiting surgeon consultation"
                    : "Consultation completed";
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
    public String getStatus() { return status; }
}
