package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A patient on the register.
 *
 * Patients may have a portal login, linked to a users row through user_id. The
 * account is separate from the clinical record on purpose — a
 * patient can change their phone number without touching their history, and
 * closing an account does not delete the visits.
 *
 * patient_no is the number the front desk says out loud ("patient 1043"). It is
 * generated, not typed, so people cannot collide on it.
 */
@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_patient_full_name", columnList = "full_name"),
        @Index(name = "idx_patient_phone", columnList = "phone"),
        @Index(name = "idx_patient_guardian_phone", columnList = "guardian_phone")
})
public class Patient extends BaseEntity {

    @Column(name = "patient_no", nullable = false, unique = true, length = 20)
    private String patientNo;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "nic", unique = true, length = 20)
    private String nic;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "dob", nullable = false)
    private LocalDate dob;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "blood_group", length = 5)
    private String bloodGroup;

    @Column(name = "guardian_name", length = 100)
    private String guardianName;

    @Column(name = "guardian_phone", length = 20)
    private String guardianPhone;

    @Column(name = "registration_notes", length = 1000)
    private String registrationNotes;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;

    @Column(name = "consent_recorded_at")
    private java.time.LocalDateTime consentRecordedAt;

    @Column(name = "consent_recorded_by", length = 50)
    private String consentRecordedBy;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "deactivated_at")
    private java.time.LocalDateTime deactivatedAt;

    @Column(name = "deactivated_by", length = 50)
    private String deactivatedBy;

    @Column(name = "deactivation_reason", length = 300)
    private String deactivationReason;

    @Column(name = "registered_on", nullable = false)
    private LocalDate registeredOn;

    public String getPatientNo() { return patientNo; }
    public void setPatientNo(String patientNo) { this.patientNo = patientNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }
    public String getGuardianPhone() { return guardianPhone; }
    public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }
    public String getRegistrationNotes() { return registrationNotes; }
    public void setRegistrationNotes(String registrationNotes) { this.registrationNotes = registrationNotes; }
    public boolean isConsentGiven() { return consentGiven; }
    public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }
    public java.time.LocalDateTime getConsentRecordedAt() { return consentRecordedAt; }
    public void setConsentRecordedAt(java.time.LocalDateTime consentRecordedAt) { this.consentRecordedAt = consentRecordedAt; }
    public String getConsentRecordedBy() { return consentRecordedBy; }
    public void setConsentRecordedBy(String consentRecordedBy) { this.consentRecordedBy = consentRecordedBy; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public java.time.LocalDateTime getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(java.time.LocalDateTime deactivatedAt) { this.deactivatedAt = deactivatedAt; }
    public String getDeactivatedBy() { return deactivatedBy; }
    public void setDeactivatedBy(String deactivatedBy) { this.deactivatedBy = deactivatedBy; }
    public String getDeactivationReason() { return deactivationReason; }
    public void setDeactivationReason(String deactivationReason) { this.deactivationReason = deactivationReason; }

    public LocalDate getRegisteredOn() { return registeredOn; }
    public void setRegisteredOn(LocalDate registeredOn) { this.registeredOn = registeredOn; }
}
