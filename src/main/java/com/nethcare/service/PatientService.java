package com.nethcare.service;

import com.nethcare.dto.PatientForm;
import com.nethcare.dto.PatientRegistrationResult;
import com.nethcare.dto.TemporaryCredential;
import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.AuditAction;
import com.nethcare.model.NumberSequence;
import com.nethcare.model.Patient;
import com.nethcare.model.Role;
import com.nethcare.model.User;
import com.nethcare.repository.NumberSequenceRepository;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

@Service
public class PatientService {

    private static final String PATIENT_SEQUENCE = "PATIENT";
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<String> BLOOD_GROUPS = Set.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PatientRepository patients;
    private final UserRepository users;
    private final NumberSequenceRepository sequences;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public PatientService(PatientRepository patients, UserRepository users,
                          NumberSequenceRepository sequences, PasswordEncoder encoder,
                          AuditService audit) {
        this.patients = patients;
        this.users = users;
        this.sequences = sequences;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional
    public PatientRegistrationResult register(PatientForm form, String actor) {
        ValidatedPatient values = validate(form, null);
        Patient patient = new Patient();
        patient.setPatientNo(nextPatientNo());
        apply(patient, values);
        patient.setRegisteredOn(LocalDate.now());
        patient.setConsentGiven(true);
        patient.setConsentRecordedAt(LocalDateTime.now());
        patient.setConsentRecordedBy(actor);
        users.findByUsername(actor == null ? "" : actor)
                .ifPresent(user -> patient.setCreatedByUserId(user.getId()));

        TemporaryCredential credential = null;
        if (form.isCreateLogin()) {
            CreatedLogin login = createLogin(patient);
            credential = login.credential();
            patient.setUserId(login.userId());
        }
        Patient saved = patients.save(patient);
        audit.record(actor, AuditAction.CREATE, "Patient", saved.getId().toString(), null,
                summary(saved), "Registered " + saved.getPatientNo());
        return new PatientRegistrationResult(saved, credential);
    }

    @Transactional
    public Patient update(Long id, PatientForm form, String actor) {
        Patient patient = get(id);
        String before = summary(patient);
        ValidatedPatient values = validate(form, id);
        apply(patient, values);
        Patient saved = patients.save(patient);
        audit.record(actor, AuditAction.UPDATE, "Patient", id.toString(), before, summary(saved));
        return saved;
    }

    @Transactional
    public Patient deactivate(Long id, String reason, String actor) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A deactivation reason is required.");
        }
        Patient patient = get(id);
        if (!Boolean.TRUE.equals(patient.getIsActive())) {
            throw new BusinessException("Patient is already inactive.");
        }
        String before = summary(patient);
        patient.setIsActive(false);
        patient.setDeactivatedAt(LocalDateTime.now());
        patient.setDeactivatedBy(actor);
        patient.setDeactivationReason(reason.trim());
        if (patient.getUserId() != null) {
            users.findById(patient.getUserId()).ifPresent(user -> {
                user.setIsActive(false);
                users.save(user);
            });
        }
        Patient saved = patients.save(patient);
        audit.record(actor, AuditAction.DELETE, "Patient", id.toString(), before,
                "inactive", reason.trim());
        return saved;
    }

    @Transactional
    public Patient reactivate(Long id, String actor) {
        Patient patient = get(id);
        patient.setIsActive(true);
        patient.setDeactivatedAt(null);
        patient.setDeactivatedBy(null);
        patient.setDeactivationReason(null);
        if (patient.getUserId() != null) {
            users.findById(patient.getUserId()).ifPresent(user -> {
                user.setIsActive(true);
                users.save(user);
            });
        }
        Patient saved = patients.save(patient);
        audit.record(actor, AuditAction.UPDATE, "Patient", id.toString(), "inactive", "active",
                "Patient reactivated");
        return saved;
    }

    @Transactional(readOnly = true)
    public Patient get(Long id) {
        return patients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No patient with id " + id));
    }

    private ValidatedPatient validate(PatientForm form, Long patientId) {
        if (patientId == null && !form.isConsentGiven()) {
            throw new BusinessException("Patient or guardian consent must be confirmed before registration.");
        }
        String fullName = trim(form.getFullName());
        if (fullName == null) throw new BusinessException("Full name is required.");
        if (fullName.length() > 100) throw new BusinessException("Full name cannot exceed 100 characters.");

        LocalDate dob;
        try {
            dob = LocalDate.parse(trim(form.getDob()));
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new BusinessException("A valid date of birth is required.");
        }
        LocalDate today = LocalDate.now();
        if (dob.isAfter(today)) throw new BusinessException("Date of birth cannot be in the future.");
        if (Period.between(dob, today).getYears() > 130) throw new BusinessException("Date of birth is outside the supported range.");

        boolean minor = Period.between(dob, today).getYears() < 18;
        String nic = normalizeNic(form.getNic());
        String phone = normalizePhone(form.getPhone(), "Phone");
        String guardianName = trim(form.getGuardianName());
        String guardianPhone = normalizePhone(form.getGuardianPhone(), "Guardian phone");

        if (!minor) {
            if (nic == null) throw new BusinessException("NIC is required for an adult patient.");
            if (!nic.matches("(?:\\d{9}[VX]|\\d{12})")) throw new BusinessException("NIC must be a valid old or new Sri Lankan NIC.");
            if (phone == null) throw new BusinessException("Phone is required for an adult patient.");
        } else {
            if (guardianName == null) throw new BusinessException("Guardian name is required for a patient under 18.");
            if (guardianPhone == null) throw new BusinessException("Guardian phone is required for a patient under 18.");
        }

        Long excludedId = patientId == null ? -1L : patientId;
        if (nic != null && patients.existsByNicIgnoreCaseAndIdNot(nic, excludedId)) {
            throw new BusinessException("A patient with this NIC is already registered.");
        }
        if (minor && patients.existsByFullNameIgnoreCaseAndDobAndGuardianPhoneAndIdNot(
                fullName, dob, guardianPhone, excludedId)) {
            throw new BusinessException("This minor is already registered with the same guardian phone.");
        }

        String bloodGroup = upper(form.getBloodGroup());
        if (bloodGroup != null && !BLOOD_GROUPS.contains(bloodGroup)) {
            throw new BusinessException("Blood group is invalid.");
        }
        String email = lower(form.getEmail());
        if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException("Email address is invalid.");
        }
        return new ValidatedPatient(fullName, nic, dob, trim(form.getGender()), phone, email,
                trim(form.getAddress()), bloodGroup, guardianName, guardianPhone,
                trim(form.getRegistrationNotes()));
    }

    private void apply(Patient patient, ValidatedPatient v) {
        patient.setFullName(v.fullName());
        patient.setNic(v.nic());
        patient.setDob(v.dob());
        patient.setGender(v.gender());
        patient.setPhone(v.phone());
        patient.setEmail(v.email());
        patient.setAddress(v.address());
        patient.setBloodGroup(v.bloodGroup());
        patient.setGuardianName(v.guardianName());
        patient.setGuardianPhone(v.guardianPhone());
        patient.setRegistrationNotes(v.registrationNotes());
    }

    private String nextPatientNo() {
        NumberSequence sequence = sequences.lockByName(PATIENT_SEQUENCE)
                .orElseGet(() -> {
                    NumberSequence created = new NumberSequence();
                    created.setSequenceName(PATIENT_SEQUENCE);
                    created.setNextValue(1L);
                    return sequences.save(created);
                });
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequences.save(sequence);
        return "P-" + String.format("%04d", value);
    }

    private CreatedLogin createLogin(Patient patient) {
        String username = uniqueUsername(patient.getFullName());
        String password = readableTemporaryPassword(patient.getFullName(), patient.getDob());
        User user = new User(username, encoder.encode(password), Role.PATIENT);
        user.setFullName(patient.getFullName());
        user.setEmail(patient.getEmail());
        user.setPhone(patient.getPhone() != null ? patient.getPhone() : patient.getGuardianPhone());
        user.setMustChangePassword(true);
        user.setIsActive(true);
        User saved = users.save(user);
        return new CreatedLogin(saved.getId(), new TemporaryCredential(username, password));
    }

    private String uniqueUsername(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".")
                .replaceAll("(^\\.|\\.$)", "");
        if (base.isBlank()) base = "patient";
        if (base.length() > 40) base = base.substring(0, 40);
        String candidate = base;
        int suffix = 2;
        while (users.findByUsername(candidate).isPresent()) candidate = base + "." + suffix++;
        return candidate;
    }

    private String readableTemporaryPassword(String name, LocalDate dob) {
        String first = name.strip().split("\\s+")[0].replaceAll("[^A-Za-z]", "");
        if (first.isBlank()) first = "Patient";
        first = first.substring(0, Math.min(first.length(), 12));
        StringBuilder random = new StringBuilder(6);
        for (int i = 0; i < 6; i++) random.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        return first + "@" + dob.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + random;
    }

    private String normalizeNic(String value) {
        String nic = upper(value);
        return nic == null ? null : nic.replace(" ", "");
    }

    private String normalizePhone(String value, String label) {
        String phone = trim(value);
        if (phone == null) return null;
        phone = phone.replaceAll("[\\s()-]", "");
        if (phone.matches("0\\d{9}")) phone = "+94" + phone.substring(1);
        else if (phone.matches("94\\d{9}")) phone = "+" + phone;
        if (!phone.matches("\\+94\\d{9}")) throw new BusinessException(label + " must be a valid Sri Lankan phone number.");
        return phone;
    }

    private String summary(Patient patient) {
        return "patientNo=" + patient.getPatientNo() + ", name=" + patient.getFullName()
                + ", nic=" + patient.getNic() + ", active=" + patient.getIsActive();
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String upper(String value) { String v = trim(value); return v == null ? null : v.toUpperCase(Locale.ROOT); }
    private String lower(String value) { String v = trim(value); return v == null ? null : v.toLowerCase(Locale.ROOT); }

    private record ValidatedPatient(String fullName, String nic, LocalDate dob, String gender,
                                    String phone, String email, String address, String bloodGroup,
                                    String guardianName, String guardianPhone, String registrationNotes) { }
    private record CreatedLogin(Long userId, TemporaryCredential credential) { }
}
