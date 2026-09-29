package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.Examination;
import com.nethcare.model.Prescription;
import com.nethcare.model.Referral;
import com.nethcare.repository.ExaminationRepository;
import com.nethcare.repository.PrescriptionRepository;
import com.nethcare.repository.ReferralRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The rules that sit between the screens and the tables.
 */
@Service
public class ClinicalService {

    private static final int ATTACHED_PRESCRIPTIONS = 4;

    private final ExaminationRepository exams;
    private final PrescriptionRepository prescriptions;
    private final ReferralRepository referrals;

    public ClinicalService(ExaminationRepository exams,
                           PrescriptionRepository prescriptions,
                           ReferralRepository referrals) {
        this.exams = exams;
        this.prescriptions = prescriptions;
        this.referrals = referrals;
    }

    // ---- Examinations ----------------------------------------------------

    public List<Examination> historyFor(Long patientId) {
        return exams.findByPatientIdOrderByExamDateDesc(patientId);
    }

    // ---- Prescriptions ---------------------------------------------------

    public Prescription issueFrom(Long examId, String issuedBy) {
        Examination exam = exams.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("No examination with id " + examId));

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

        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A referral needs a reason.");
        }
        if (!"ROUTINE".equalsIgnoreCase(urgency) && !"URGENT".equalsIgnoreCase(urgency)) {
            throw new BusinessException("Urgency must be ROUTINE or URGENT.");
        }

        Referral r = new Referral();
        r.setRefNo(nextRefNo());
        r.setPatientId(exam.getPatientId());
        r.setExaminationId(exam.getId());
        r.setReferredOn(LocalDate.now());
        r.setReferredBy(referredBy);
        r.setSurgeonName(surgeonName);
        r.setReason(reason);
        r.setUrgency(urgency.toUpperCase());
        r.setAttachedHistory(buildHistory(exam.getPatientId()));
        return referrals.save(r);
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
