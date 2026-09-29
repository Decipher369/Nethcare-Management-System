package com.nethcare.dto;

import com.nethcare.model.Patient;

import java.time.LocalDate;

/**
 * A patient as the API hands it out.
 *
 * A copy of the entity rather than the entity itself: the JSON contract should
 * not shift every time a column is added, and it keeps the id and the
 * generated patient number separate from what the desk can edit.
 */
public class PatientDto {

    private Long id;
    private String patientNo;
    private String fullName;
    private String nic;
    private LocalDate dob;
    private String gender;
    private String phone;
    private String email;
    private String address;
    private String bloodGroup;
    private LocalDate registeredOn;
    private Long userId;
    private String guardianName;
    private String guardianPhone;
    private String registrationNotes;
    private boolean active;
    private boolean consentGiven;

    // Age is worked out on the way out so no client has to repeat the
    // arithmetic, and so it is never stored where it can go stale.
    private Integer age;

    public static PatientDto of(Patient p) {
        PatientDto d = new PatientDto();
        d.id = p.getId();
        d.patientNo = p.getPatientNo();
        d.fullName = p.getFullName();
        d.nic = p.getNic();
        d.dob = p.getDob();
        d.gender = p.getGender();
        d.phone = p.getPhone();
        d.email = p.getEmail();
        d.address = p.getAddress();
        d.bloodGroup = p.getBloodGroup();
        d.registeredOn = p.getRegisteredOn();
        d.userId = p.getUserId();
        d.guardianName = p.getGuardianName();
        d.guardianPhone = p.getGuardianPhone();
        d.registrationNotes = p.getRegistrationNotes();
        d.active = Boolean.TRUE.equals(p.getIsActive());
        d.consentGiven = p.isConsentGiven();
        d.age = (p.getDob() == null) ? null : java.time.Period.between(p.getDob(), LocalDate.now()).getYears();
        return d;
    }

    public static java.util.List<PatientDto> of(java.util.List<Patient> list) {
        return list.stream().map(PatientDto::of).toList();
    }

    public Long getId() { return id; }
    public String getPatientNo() { return patientNo; }
    public String getFullName() { return fullName; }
    public String getNic() { return nic; }
    public LocalDate getDob() { return dob; }
    public String getGender() { return gender; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getBloodGroup() { return bloodGroup; }
    public LocalDate getRegisteredOn() { return registeredOn; }
    public Long getUserId() { return userId; }
    public String getGuardianName() { return guardianName; }
    public String getGuardianPhone() { return guardianPhone; }
    public String getRegistrationNotes() { return registrationNotes; }
    public boolean isActive() { return active; }
    public boolean isConsentGiven() { return consentGiven; }
    public Integer getAge() { return age; }
}
