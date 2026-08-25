package com.nethcare.service;

import com.nethcare.dto.PatientForm;
import com.nethcare.exception.BusinessException;
import com.nethcare.model.Patient;
import com.nethcare.model.Role;
import com.nethcare.model.User;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Registers a patient and, if asked, the login that goes with it.
 */
@Service
public class PatientService {

    private final PatientRepository patients;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public PatientService(PatientRepository patients, UserRepository users, PasswordEncoder encoder) {
        this.patients = patients;
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public Patient register(PatientForm form) {
        if (form.getFullName() == null || form.getFullName().isBlank()) {
            throw new BusinessException("Full name is required.");
        }
        if (form.getDob() == null || form.getDob().isBlank()) {
            throw new BusinessException("Date of birth is required.");
        }

        LocalDate dob;
        try {
            dob = LocalDate.parse(form.getDob());
        } catch (DateTimeParseException e) {
            throw new BusinessException("Date of birth is not a valid date.");
        }
        if (dob.isAfter(LocalDate.now())) {
            throw new BusinessException("Date of birth cannot be in the future.");
        }

        Patient p = new Patient();
        p.setPatientNo(nextPatientNo());
        p.setFullName(form.getFullName().trim());
        p.setDob(dob);
        p.setGender(blankToNull(form.getGender()));
        p.setPhone(blankToNull(form.getPhone()));
        p.setEmail(blankToNull(form.getEmail()));
        p.setAddress(blankToNull(form.getAddress()));
        p.setBloodGroup(blankToNull(form.getBloodGroup()));
        p.setRegisteredOn(LocalDate.now());

        if (form.isCreateLogin()) {
            p.setUserId(createLogin(p));
        }

        return patients.save(p);
    }

    // P-nnnn, counting up from the highest existing number. The unique index on
    // patient_no is what actually stops a duplicate — two people registering at
    // once can pick the same number here and the second insert will fail.
    private String nextPatientNo() {
        return "P-" + String.format("%04d", patients.count() + 1);
    }

    // Username comes from the name so it is guessable at the desk. If it is
    // taken, append the patient number rather than failing the registration.
    private Long createLogin(Patient p) {
        String base = p.getFullName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".").replaceAll("(^\\.|\\.$)", "");
        if (base.isBlank()) {
            base = "patient";
        }
        String username = base;
        if (users.findByUsername(username).isPresent()) {
            username = base + "." + p.getPatientNo().toLowerCase(Locale.ROOT).replace("-", "");
        }

        User u = new User(username, encoder.encode(generatePassword()), Role.PATIENT);
        u.setFullName(p.getFullName());
        u.setEmail(p.getEmail());
        u.setStatus("ACTIVE");
        return users.save(u).getId();
    }

    // Patient self-service accounts get a random password and the front desk
    // writes it on a slip. A guessable one like their date of birth would let
    // anyone read someone else's portal.
    private String generatePassword() {
        return java.util.UUID.randomUUID().toString().substring(0, 10);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
