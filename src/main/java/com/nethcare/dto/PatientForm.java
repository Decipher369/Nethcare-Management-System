package com.nethcare.dto;

/**
 * What the registration form collects.
 *
 * Kept apart from the entity so the form can carry validation errors and a
 * separate "create a login for them too" tick without the entity having to
 * know about any of that.
 */
public class PatientForm {

    private String fullName;
    private String dob;
    private String gender;
    private String phone;
    private String email;
    private String address;
    private String bloodGroup;
    private boolean createLogin;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

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

    public boolean isCreateLogin() { return createLogin; }
    public void setCreateLogin(boolean createLogin) { this.createLogin = createLogin; }
}
