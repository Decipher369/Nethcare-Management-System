package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A patient on the register.
 *
 * Patients are login users too, so a registration links to a users row through
 * user_id. The account is separate from the clinical record on purpose — a
 * patient can change their phone number without touching their history, and
 * closing an account does not delete the visits.
 *
 * patient_no is the number the front desk says out loud ("patient 1043"). It is
 * generated, not typed, so people cannot collide on it.
 */
@Entity
@Table(name = "patients")
public class Patient extends BaseEntity {

    @Column(name = "patient_no", nullable = false, unique = true, length = 20)
    private String patientNo;

    @Column(name = "user_id")
    private Long userId;

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

    @Column(name = "registered_on", nullable = false)
    private LocalDate registeredOn;

    public String getPatientNo() { return patientNo; }
    public void setPatientNo(String patientNo) { this.patientNo = patientNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

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

    public LocalDate getRegisteredOn() { return registeredOn; }
    public void setRegisteredOn(LocalDate registeredOn) { this.registeredOn = registeredOn; }
}
