package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;

/** Contact-lens dispensing and replacement record linked to a patient bill. */
@Entity
@Table(name = "contact_lens_trackers", indexes = {
        @Index(name = "idx_contact_tracker_patient", columnList = "patient_id"),
        @Index(name = "idx_contact_tracker_replacement", columnList = "alert_sent, replacement_date")
})
public class ContactLensTracker extends BaseEntity {
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", insertable = false, updatable = false)
    private Patient patient;
    @Column(name = "bill_id", nullable = false)
    private Long billId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", insertable = false, updatable = false)
    private Bill bill;
    @Column(name = "lens_brand", nullable = false, length = 100)
    private String lensBrand;
    @Column(name = "lens_modality_days", nullable = false)
    private int lensModalityDays;
    @Column(name = "dispensed_date", nullable = false)
    private LocalDate dispensedDate;
    @Column(name = "replacement_date", nullable = false)
    private LocalDate replacementDate;
    @Column(name = "alert_sent", nullable = false)
    private boolean alertSent;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getLensBrand() { return lensBrand; }
    public void setLensBrand(String lensBrand) { this.lensBrand = lensBrand; }
    public int getLensModalityDays() { return lensModalityDays; }
    public void setLensModalityDays(int lensModalityDays) { this.lensModalityDays = lensModalityDays; }
    public LocalDate getDispensedDate() { return dispensedDate; }
    public void setDispensedDate(LocalDate dispensedDate) { this.dispensedDate = dispensedDate; }
    public LocalDate getReplacementDate() { return replacementDate; }
    public void setReplacementDate(LocalDate replacementDate) { this.replacementDate = replacementDate; }
    public boolean isAlertSent() { return alertSent; }
    public void setAlertSent(boolean alertSent) { this.alertSent = alertSent; }
}
