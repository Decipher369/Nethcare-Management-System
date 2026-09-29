package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.Examination;
import com.nethcare.model.ClinicalSymptom;
import com.nethcare.model.MedicalHistory;
import com.nethcare.model.Prescription;
import com.nethcare.model.Referral;
import com.nethcare.model.Role;
import com.nethcare.model.User;
import com.nethcare.repository.ClinicalSymptomRepository;
import com.nethcare.repository.ExaminationRepository;
import com.nethcare.repository.MedicalHistoryRepository;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.PrescriptionRepository;
import com.nethcare.repository.ReferralRepository;
import com.nethcare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The rules that sit between the screens and the tables.
 */
@Service
public class ClinicalService {

    private static final int ATTACHED_PRESCRIPTIONS = 4;

    private final ExaminationRepository exams;
    private final PrescriptionRepository prescriptions;
    private final ReferralRepository referrals;
    private final PatientRepository patients;
    private final ClinicalSymptomRepository symptoms;
    private final MedicalHistoryRepository histories;
    private final UserRepository users;

    public ClinicalService(ExaminationRepository exams,
                           PrescriptionRepository prescriptions,
                           ReferralRepository referrals,
                           PatientRepository patients,
                           ClinicalSymptomRepository symptoms,
                           MedicalHistoryRepository histories,
                           UserRepository users) {
        this.exams = exams;
        this.prescriptions = prescriptions;
        this.referrals = referrals;
        this.patients = patients;
        this.symptoms = symptoms;
        this.histories = histories;
        this.users = users;
    }

    // ---- Examinations ----------------------------------------------------

    public List<Examination> historyFor(Long patientId) {
        return exams.findByPatientIdOrderByExamDateDesc(patientId);
    }

    @Transactional
    public Examination recordExamination(Examination exam, ClinicalSymptom symptom,
                                         MedicalHistory history, String clinician) {
        if (exam.getPatientId() == null || !patients.existsById(exam.getPatientId())) {
            throw new ResourceNotFoundException("Select a registered patient before recording an examination.");
        }
        LocalDate examDate = exam.getExamDate() == null ? LocalDate.now() : exam.getExamDate();
        if (examDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Exam date cannot be in the future.");
        }

        validatePower("Right sphere", exam.getOdSph(), -40, 40);
        validatePower("Right cylinder", exam.getOdCyl(), -20, 20);
        validateAxis("Right axis", exam.getOdAxis());
        validatePower("Right add", exam.getOdAdd(), 0, 10);
        validatePower("Left sphere", exam.getOsSph(), -40, 40);
        validatePower("Left cylinder", exam.getOsCyl(), -20, 20);
        validateAxis("Left axis", exam.getOsAxis());
        validatePower("Left add", exam.getOsAdd(), 0, 10);
        validatePower("PD", exam.getIpd(), 30, 90);

        if (!hasClinicalData(exam, symptom)) {
            throw new BusinessException("Record a symptom, clinical finding, or eye measurement.");
        }

        exam.setId(null);
        exam.setExamDate(examDate);
        exam.setExaminedBy(clinician);
        Examination saved = exams.save(exam);

        symptom.setId(null);
        symptom.setExaminationId(saved.getId());
        symptoms.save(symptom);

        history.setId(null);
        history.setPatientId(saved.getPatientId());
        history.setExaminationId(saved.getId());
        history.setVersionNumber(Math.toIntExact(histories.countByPatientId(saved.getPatientId()) + 1));
        history.setRecordedOn(examDate);
        history.setRecordedBy(clinician);
        histories.save(history);
        return saved;
    }

    public Optional<ClinicalSymptom> symptomsFor(Long examinationId) {
        return symptoms.findByExaminationId(examinationId);
    }

    public List<MedicalHistory> medicalHistoryFor(Long patientId) {
        return histories.findByPatientIdOrderByVersionNumberDesc(patientId);
    }

    public Optional<MedicalHistory> currentMedicalHistory(Long patientId) {
        return histories.findFirstByPatientIdOrderByVersionNumberDesc(patientId);
    }

    public Optional<MedicalHistory> medicalHistoryForExamination(Long examinationId) {
        return histories.findByExaminationId(examinationId);
    }

    public MedicalHistory historyDraftFor(Long patientId) {
        MedicalHistory draft = new MedicalHistory();
        currentMedicalHistory(patientId).ifPresent(current -> copyHistory(current, draft));
        return draft;
    }

    private void copyHistory(MedicalHistory from, MedicalHistory to) {
        to.setDiabetic(from.isDiabetic());
        to.setAsthma(from.isAsthma());
        to.setHypertension(from.isHypertension());
        to.setCardiac(from.isCardiac());
        to.setSle(from.isSle());
        to.setCholesterol(from.isCholesterol());
        to.setTb(from.isTb());
        to.setThyroid(from.isThyroid());
        to.setArthritis(from.isArthritis());
        to.setSyphilis(from.isSyphilis());
        to.setCancer(from.isCancer());
        to.setRenal(from.isRenal());
        to.setBronchitis(from.isBronchitis());
        to.setMigraine(from.isMigraine());
        to.setOcularHistory(from.getOcularHistory());
        to.setOtherConditions(from.getOtherConditions());
    }

    // ---- Prescriptions ---------------------------------------------------

    public Prescription issueFrom(Long examId, String issuedBy) {
        Examination exam = exams.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("No examination with id " + examId));

        if (prescriptions.existsByExaminationId(examId)) {
            throw new BusinessException("A prescription has already been issued from this examination.");
        }

        Prescription rx = new Prescription();
        rx.setRxNo(nextRxNo());
        rx.setPatientId(exam.getPatientId());
        rx.setExaminationId(exam.getId());
        rx.setIssuedOn(LocalDate.now());
        rx.setIssuedBy(issuedBy);
        copyPower(exam, rx);
        return prescriptions.save(rx);
    }

    // The prescription carries the numbers as written, so a later comparison
    // shows what actually changed rather than a re-rounded copy.
    private void copyPower(Examination exam, Prescription rx) {
        rx.setOdSph(exam.getOdSph());
        rx.setOdCyl(exam.getOdCyl());
        rx.setOdAxis(exam.getOdAxis());
        rx.setOdAdd(exam.getOdAdd());
        rx.setOsSph(exam.getOsSph());
        rx.setOsCyl(exam.getOsCyl());
        rx.setOsAxis(exam.getOsAxis());
        rx.setOsAdd(exam.getOsAdd());
        rx.setIpd(exam.getIpd());
    }

    public List<Prescription> prescriptionsFor(Long patientId) {
        return prescriptions.findByPatientIdOrderByIssuedOnDesc(patientId);
    }

    // ---- Referrals -------------------------------------------------------

    public Referral refer(Long examId, String referredBy, String surgeonName,
                          String reason, String urgency) {
        Examination exam = exams.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("No examination with id " + examId));

        if (referrals.existsByExaminationId(examId)) {
            throw new BusinessException("A referral has already been created from this examination.");
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A referral needs a reason.");
        }
        User surgeon = users.findByUsername(surgeonName)
                .filter(user -> user.getRole() == Role.SURGEON && user.isActive())
                .orElseThrow(() -> new BusinessException("Select an active surgeon account."));
        if (!"ROUTINE".equalsIgnoreCase(urgency) && !"URGENT".equalsIgnoreCase(urgency)) {
            throw new BusinessException("Urgency must be ROUTINE or URGENT.");
        }

        Referral r = new Referral();
        r.setRefNo(nextRefNo());
        r.setPatientId(exam.getPatientId());
        r.setExaminationId(exam.getId());
        r.setReferredOn(LocalDate.now());
        r.setReferredBy(referredBy);
        r.setSurgeonName(surgeon.getUsername());
        r.setReason(reason);
        r.setUrgency(urgency.toUpperCase());
        r.setAttachedHistory(buildHistory(exam.getPatientId()));
        return referrals.save(r);
    }

    private void validatePower(String label, String value, int minimum, int maximum) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            BigDecimal number = new BigDecimal(value.trim());
            if (number.compareTo(BigDecimal.valueOf(minimum)) < 0
                    || number.compareTo(BigDecimal.valueOf(maximum)) > 0) {
                throw new BusinessException(label + " must be between " + minimum + " and " + maximum + ".");
            }
        } catch (NumberFormatException ex) {
            throw new BusinessException(label + " must be a number.");
        }
    }

    private void validateAxis(String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            int axis = Integer.parseInt(value.trim());
            if (axis < 0 || axis > 180) {
                throw new BusinessException(label + " must be between 0 and 180 degrees.");
            }
        } catch (NumberFormatException ex) {
            throw new BusinessException(label + " must be a whole number between 0 and 180.");
        }
    }

    private boolean hasClinicalData(Examination e, ClinicalSymptom s) {
        return isSet(e.getOdSph()) || isSet(e.getOdCyl()) || isSet(e.getOdAxis())
                || isSet(e.getOdVa()) || isSet(e.getOsSph()) || isSet(e.getOsCyl())
                || isSet(e.getOsAxis()) || isSet(e.getOsVa()) || isSet(e.getFindings())
                || s.isHeadache() || s.isBlurVision() || s.isTearing() || s.isRedness()
                || s.isItching() || s.isStrain() || s.isFloaters() || s.isFlashes()
                || s.isBurning() || s.isDischarge() || s.isDoubleVision() || s.isSquint()
                || s.isGlare() || s.isPain() || s.isDistortion() || s.isDry()
                || s.isPhotophobia() || isSet(s.getAdditionalSymptoms());
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    // One block of text the surgeon can read without opening anything else.
    private String buildHistory(Long patientId) {
        List<Prescription> last = prescriptions.findTop4ByPatientIdOrderByIssuedOnDesc(patientId);
        if (last.isEmpty()) {
            return "No previous prescriptions on file.";
        }
        StringBuilder sb = new StringBuilder();
        for (Prescription rx : last) {
            sb.append("Rx ").append(rx.getRxNo())
              .append("  ").append(rx.getIssuedOn())
              .append("   R ").append(join(rx.getOdSph(), rx.getOdCyl(), rx.getOdAxis()))
              .append("   L ").append(join(rx.getOsSph(), rx.getOsCyl(), rx.getOsAxis()))
              .append('\n');
        }
        return sb.toString().trim();
    }

    private String join(String sph, String cyl, String axis) {
        StringBuilder sb = new StringBuilder();
        if (sph != null) { sb.append(sph); }
        if (cyl != null) { sb.append(' ').append(cyl); }
        if (axis != null) { sb.append(" x ").append(axis); }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    public void recordFeedback(Long referralId, String feedback) {
        Referral r = referrals.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("No referral with id " + referralId));

        if (r.getFeedback() != null) {
            throw new BusinessException("Feedback has already been recorded for this referral.");
        }
        if (feedback == null || feedback.isBlank()) {
            throw new BusinessException("Surgical notes are required.");
        }

        r.setFeedback(feedback);
        r.setFeedbackOn(LocalDate.now());
        r.setStatus("COMPLETED");
        referrals.save(r);
    }

    @Transactional
    public Referral recordConsultation(Long referralId, String actor, boolean admin,
                                       String tests, String diagnosis, String treatment,
                                       String followUpInstructions, String notes) {
        Referral referral = referrals.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("No referral with id " + referralId));
        if (!admin && !referral.getSurgeonName().equalsIgnoreCase(actor)) {
            throw new BusinessException("This referral is assigned to another surgeon.");
        }
        if ("COMPLETED".equalsIgnoreCase(referral.getStatus())) {
            throw new BusinessException("This consultation has already been completed.");
        }
        if (!isSet(diagnosis)) {
            throw new BusinessException("Diagnosis is required to complete the consultation.");
        }
        if (!isSet(treatment)) {
            throw new BusinessException("Treatment is required to complete the consultation.");
        }

        referral.setTests(blankToNull(tests));
        referral.setDiagnosis(diagnosis.trim());
        referral.setTreatment(treatment.trim());
        referral.setFollowUpInstructions(blankToNull(followUpInstructions));
        referral.setFeedback(blankToNull(notes));
        referral.setFeedbackOn(LocalDate.now());
        referral.setStatus("COMPLETED");
        return referrals.save(referral);
    }

    private String blankToNull(String value) {
        return isSet(value) ? value.trim() : null;
    }

    public List<Referral> referralsFor(Long patientId) {
        return referrals.findByPatientIdOrderByReferredOnDesc(patientId);
    }

    // ---- Comparison ------------------------------------------------------

    /**
     * Deltas between two prescriptions, the way the history table shows them.
     * Moved out of the controller so both the API and any later view read the
     * same wording.
     */
    public Map<String, String> compareRx(Long rx1Id, Long rx2Id) {
        Prescription a = prescriptions.findById(rx1Id)
                .orElseThrow(() -> new ResourceNotFoundException("No prescription " + rx1Id));
        Prescription b = prescriptions.findById(rx2Id)
                .orElseThrow(() -> new ResourceNotFoundException("No prescription " + rx2Id));

        return Map.of(
                "rx1", a.getRxNo(),
                "rx2", b.getRxNo(),
                "odSph", delta(a.getOdSph(), b.getOdSph()),
                "osSph", delta(a.getOsSph(), b.getOsSph()),
                "odCyl", delta(a.getOdCyl(), b.getOdCyl()),
                "osCyl", delta(a.getOsCyl(), b.getOsCyl()));
    }

    // Compared as written text, not parsed as numbers — -0.25 and 0.00 are
    // both "no correction" and should read as unchanged.
    private String delta(String older, String newer) {
        if (older == null || older.isBlank()) {
            return newer == null || newer.isBlank() ? "unchanged" : "new: " + newer;
        }
        if (newer == null || newer.isBlank()) {
            return "removed";
        }
        return older.equals(newer) ? "unchanged" : older + " → " + newer;
    }

    // ---- Numbering -------------------------------------------------------

    // Counts up from what is already there. The unique index on rx_no / ref_no
    // is what actually stops a duplicate if two opticians issue at the same
    // moment — the second insert fails and the client retries.
    private String nextRxNo() {
        return "RX-" + String.format("%05d", prescriptions.count() + 1);
    }

    private String nextRefNo() {
        return "REF-" + String.format("%05d", referrals.count() + 1);
    }
}
