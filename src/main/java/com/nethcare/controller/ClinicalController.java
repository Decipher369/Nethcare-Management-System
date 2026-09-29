package com.nethcare.controller;

import com.nethcare.dto.ApiResponse;
import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.Examination;
import com.nethcare.model.ClinicalSymptom;
import com.nethcare.model.MedicalHistory;
import com.nethcare.model.Prescription;
import com.nethcare.model.Referral;
import com.nethcare.repository.ExaminationRepository;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.PrescriptionRepository;
import com.nethcare.repository.ReferralRepository;
import com.nethcare.service.ClinicalService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * M2 — examinations, prescriptions, referrals.
 */
@RestController
public class ClinicalController {

    private final ClinicalService clinical;
    private final ExaminationRepository exams;
    private final PrescriptionRepository prescriptions;
    private final ReferralRepository referrals;
    private final PatientRepository patients;

    public ClinicalController(ClinicalService clinical,
                              ExaminationRepository exams,
                              PrescriptionRepository prescriptions,
                              ReferralRepository referrals,
                              PatientRepository patients) {
        this.clinical = clinical;
        this.exams = exams;
        this.prescriptions = prescriptions;
        this.referrals = referrals;
        this.patients = patients;
    }

    // ---- Examinations ----------------------------------------------------

    @PostMapping("/api/examinations")
    public ApiResponse<Examination> record(@RequestBody Examination exam,
                                           Authentication auth) {
        MedicalHistory history = exam.getPatientId() == null
                ? new MedicalHistory()
                : clinical.historyDraftFor(exam.getPatientId());
        return ApiResponse.success(clinical.recordExamination(
                exam, new ClinicalSymptom(), history, auth.getName()));
    }

    @GetMapping("/api/examinations/{id}")
    public ApiResponse<Examination> get(@PathVariable Long id) {
        return ApiResponse.success(exams.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No examination with id " + id)));
    }

    @GetMapping("/api/patients/{id}/examinations")
    public ApiResponse<List<Examination>> forPatient(@PathVariable Long id) {
        return ApiResponse.success(clinical.historyFor(id));
    }

    // ---- Prescriptions ---------------------------------------------------

    @PostMapping("/api/examinations/{id}/prescription")
    public ApiResponse<Prescription> issue(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success(clinical.issueFrom(id, auth.getName()));
    }

    @GetMapping("/api/prescriptions/{id}")
    public ApiResponse<Prescription> getPrescription(@PathVariable Long id) {
        return ApiResponse.success(prescriptions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No prescription with id " + id)));
    }

    @GetMapping("/api/patients/{id}/prescriptions")
    public ApiResponse<List<Prescription>> prescriptionsFor(@PathVariable Long id) {
        return ApiResponse.success(clinical.prescriptionsFor(id));
    }

    // Deltas between two prescriptions, the way the history table shows them.
    @GetMapping("/api/prescriptions/compare")
    public ApiResponse<Map<String, String>> compare(@RequestParam Long rx1, @RequestParam Long rx2) {
        return ApiResponse.success(clinical.compareRx(rx1, rx2));
    }

    // ---- Referrals -------------------------------------------------------

    @PostMapping("/api/examinations/{id}/referral")
    public ApiResponse<Referral> refer(@PathVariable Long id,
                                       @RequestParam String surgeon,
                                       @RequestParam String reason,
                                       @RequestParam(defaultValue = "ROUTINE") String urgency,
                                       Authentication auth) {
        return ApiResponse.success(clinical.refer(id, auth.getName(), surgeon, reason, urgency));
    }

    @GetMapping("/api/referrals/{id}")
    public ApiResponse<Referral> getReferral(@PathVariable Long id) {
        return ApiResponse.success(referrals.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No referral with id " + id)));
    }

    // Feedback, the worklists and the patient's referral list all moved to
    // ReferralApiController. The feedback call there also checks the caller is
    // the surgeon, which this version did not do.
}
