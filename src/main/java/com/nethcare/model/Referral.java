package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A referral to a surgeon.
 *
 * The surgeon should not have to go and find the history, so a referral always
 * carries the last four prescriptions with it. They are copied in as text at
 * the point the referral is made — not looked up live — because a referral is
 * a record of what the surgeon was sent at that time. If the patient's record
 * changes later, the referral still says what was actually handed over.
 *
 * Feedback from the surgeon is written once. After that the referral is closed
 * and neither side can edit it.
 */
@Entity
@Table(name = "referrals")
public class Referral extends BaseEntity {

    @Column(name = "ref_no", nullable = false, unique = true, length = 20)
    private String refNo;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "examination_id")
    private Long examinationId;

    @Column(name = "referred_on", nullable = false)
    private LocalDate referredOn;

    @Column(name = "referred_by", nullable = false, length = 100)
    private String referredBy;

    @Column(name = "surgeon_name", nullable = false, length = 100)
    private String surgeonName;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "diagnosis", length = 500)
    private String diagnosis;

    // ROUTINE or URGENT
    @Column(name = "urgency", nullable = false, length = 10)
    private String urgency;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    // The last four prescriptions, copied in as text. See the class comment.
    @Column(name = "attached_history", length = 4000)
    private String attachedHistory;

    @Column(name = "feedback", length = 2000)
    private String feedback;

    @Column(name = "feedback_on")
    private LocalDate feedbackOn;

    @Column(name = "tests", length = 2000)
    private String tests;

    @Column(name = "treatment", length = 2000)
    private String treatment;

    @Column(name = "follow_up_instructions", length = 2000)
    private String followUpInstructions;

    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getExaminationId() { return examinationId; }
    public void setExaminationId(Long examinationId) { this.examinationId = examinationId; }

    public LocalDate getReferredOn() { return referredOn; }
    public void setReferredOn(LocalDate referredOn) { this.referredOn = referredOn; }

    public String getReferredBy() { return referredBy; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }

    public String getSurgeonName() { return surgeonName; }
    public void setSurgeonName(String surgeonName) { this.surgeonName = surgeonName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAttachedHistory() { return attachedHistory; }
    public void setAttachedHistory(String attachedHistory) { this.attachedHistory = attachedHistory; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDate getFeedbackOn() { return feedbackOn; }
    public void setFeedbackOn(LocalDate feedbackOn) { this.feedbackOn = feedbackOn; }
    public String getTests() { return tests; }
    public void setTests(String tests) { this.tests = tests; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }
    public String getFollowUpInstructions() { return followUpInstructions; }
    public void setFollowUpInstructions(String followUpInstructions) { this.followUpInstructions = followUpInstructions; }
}
