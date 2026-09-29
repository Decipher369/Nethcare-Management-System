package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A patient who is due back, and what happened when we asked.
 *
 * patient_id points at the patients table in M1. The association is a plain
 * column rather than a JPA relationship because M4 is built on its own branch
 * and the Patient class does not exist in this tree yet — see the note on
 * Notification for why that matters.
 *
 * target_review_date is explicitly chosen from the clinician's advice or a
 * staff decision. It is never inferred from a fixed prescription-validity rule.
 */
@Entity
@Table(name = "patient_followup_cases")
public class FollowUp extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "assigned_optician_id", nullable = false)
    private Long assignedOpticianId;

    @Column(name = "originating_visit_id", nullable = false)
    private Long originatingVisitId;

    @Column(name = "patient_name", length = 120)
    private String patientName;

    @Column(name = "last_exam_on")
    private LocalDate lastExamOn;

    @Column(name = "target_review_date", nullable = false)
    private LocalDate dueOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_category", nullable = false, length = 40)
    private FollowUpCategory category;

    @Column(name = "is_high_risk", nullable = false)
    private boolean highRisk;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 120)
    private String email;

    /** How the patient replied. Null until somebody gets an answer. */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", columnDefinition = "VARCHAR(20)")
    private FollowUpOutcome outcome;

    @Column(name = "clinical_notes", length = 1000)
    private String responseNote;

    @Column(name = "responded_on")
    private LocalDate respondedOn;

    @Column(name = "booked_on")
    private LocalDate bookedOn;

    @Column(name = "opt_out", nullable = false)
    private Boolean optOut = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
    private FollowUpStatus status = FollowUpStatus.ACTIVE;

    /**
     * False when the contact details on file will not reach anybody. A
     * reminder is still queued so the gap is visible, but the screen sorts
     * these to the top because a bad number is a call that has to be made.
     */
    public boolean isContactReachable() {
        if (phone == null) return false;
        String normalized = phone.replaceAll("[\\s()-]", "");
        return normalized.matches("07\\d{8}") || normalized.matches("\\+947\\d{8}");
    }

    public boolean isOpen() {
        return status != null && status.isOpen();
    }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public LocalDate getLastExamOn() { return lastExamOn; }
    public void setLastExamOn(LocalDate lastExamOn) { this.lastExamOn = lastExamOn; }

    public LocalDate getDueOn() { return dueOn; }
    public void setDueOn(LocalDate dueOn) { this.dueOn = dueOn; }

    public String getDueForWho() { return category == null ? "Uncategorised" : category.label(); }

    public Long getAssignedOpticianId() { return assignedOpticianId; }
    public void setAssignedOpticianId(Long assignedOpticianId) { this.assignedOpticianId = assignedOpticianId; }

    public Long getOriginatingVisitId() { return originatingVisitId; }
    public void setOriginatingVisitId(Long originatingVisitId) { this.originatingVisitId = originatingVisitId; }

    public FollowUpCategory getCategory() { return category; }
    public void setCategory(FollowUpCategory category) { this.category = category; }

    public boolean isHighRisk() { return highRisk; }
    public void setHighRisk(boolean highRisk) { this.highRisk = highRisk; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public FollowUpOutcome getOutcome() { return outcome; }
    public void setOutcome(FollowUpOutcome outcome) { this.outcome = outcome; }

    public String getResponseNote() { return responseNote; }
    public void setResponseNote(String responseNote) { this.responseNote = responseNote; }

    public LocalDate getRespondedOn() { return respondedOn; }
    public void setRespondedOn(LocalDate respondedOn) { this.respondedOn = respondedOn; }

    public LocalDate getBookedOn() { return bookedOn; }
    public void setBookedOn(LocalDate bookedOn) { this.bookedOn = bookedOn; }

    public Boolean getOptOut() { return optOut; }
    public void setOptOut(Boolean optOut) { this.optOut = optOut; }

    public FollowUpStatus getStatus() { return status; }
    public void setStatus(FollowUpStatus status) { this.status = status; }
}
