package com.nethcare.controller;

import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.ClinicalSymptom;
import com.nethcare.model.Examination;
import com.nethcare.model.MedicalHistory;
import com.nethcare.model.Patient;
import com.nethcare.repository.ExaminationRepository;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.PrescriptionRepository;
import com.nethcare.repository.ReferralRepository;
import com.nethcare.repository.UserRepository;
import com.nethcare.model.Role;
import com.nethcare.service.ClinicalService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** Browser workflow used by the optician during an eye examination. */
@Controller
public class ClinicalWorkflowController {

    private final ClinicalService clinical;
    private final ExaminationRepository examinations;
    private final PatientRepository patients;
    private final PrescriptionRepository prescriptions;
    private final ReferralRepository referrals;
    private final UserRepository users;

    public ClinicalWorkflowController(ClinicalService clinical,
                                      ExaminationRepository examinations,
                                      PatientRepository patients,
                                      PrescriptionRepository prescriptions,
                                      ReferralRepository referrals,
                                      UserRepository users) {
        this.clinical = clinical;
        this.examinations = examinations;
        this.patients = patients;
        this.prescriptions = prescriptions;
        this.referrals = referrals;
        this.users = users;
    }

    @GetMapping("/examinations/new")
    public String newExamination(@RequestParam Long patientId, Model model,
                                 Authentication authentication) {
        Patient patient = patient(patientId);
        Examination exam = new Examination();
        exam.setPatientId(patientId);
        exam.setExamDate(LocalDate.now());
        populateForm(model, patient, exam, new ClinicalSymptom(),
                clinical.historyDraftFor(patientId), authentication.getName());
        return "clinical/examination-form";
    }

    @PostMapping("/examinations")
    public String record(@ModelAttribute("exam") Examination exam,
                         @ModelAttribute("symptoms") ClinicalSymptom symptoms,
                         @ModelAttribute("history") MedicalHistory history,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirect) {
        try {
            Examination saved = clinical.recordExamination(
                    exam, symptoms, history, authentication.getName());
            redirect.addFlashAttribute("msg", "Examination recorded.");
            return "redirect:/examinations/" + saved.getId();
        } catch (BusinessException | ResourceNotFoundException ex) {
            Patient patient = exam.getPatientId() == null ? null : patient(exam.getPatientId());
            populateForm(model, patient, exam, symptoms, history, authentication.getName());
            model.addAttribute("error", ex.getMessage());
            return "clinical/examination-form";
        }
    }

    @GetMapping("/examinations/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication authentication) {
        Examination exam = examination(id);
        model.addAttribute("exam", exam);
        model.addAttribute("patient", patient(exam.getPatientId()));
        model.addAttribute("symptoms", clinical.symptomsFor(id).orElse(new ClinicalSymptom()));
        model.addAttribute("history", clinical.medicalHistoryForExamination(id).orElse(new MedicalHistory()));
        model.addAttribute("prescription", prescriptions.findByExaminationId(id).orElse(null));
        model.addAttribute("referral", referrals.findByExaminationId(id).orElse(null));
        model.addAttribute("surgeons", users.findByRoleOrderByFullNameAsc(Role.SURGEON));
        model.addAttribute("user", authentication.getName());
        return "clinical/examination-detail";
    }

    @PostMapping("/examinations/{id}/prescription")
    public String issuePrescription(@PathVariable Long id, Authentication authentication,
                                    RedirectAttributes redirect) {
        try {
            clinical.issueFrom(id, authentication.getName());
            redirect.addFlashAttribute("msg", "Prescription issued and added to the permanent history.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/examinations/" + id;
    }

    private void populateForm(Model model, Patient patient, Examination exam,
                              ClinicalSymptom symptoms, MedicalHistory history, String username) {
        model.addAttribute("patient", patient);
        model.addAttribute("exam", exam);
        model.addAttribute("symptoms", symptoms);
        model.addAttribute("history", history);
        model.addAttribute("user", username);
    }

    private Patient patient(Long id) {
        return patients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No patient with id " + id));
    }

    private Examination examination(Long id) {
        return examinations.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No examination with id " + id));
    }
}
