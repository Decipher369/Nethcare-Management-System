package com.nethcare.controller;

import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.Referral;
import com.nethcare.repository.ExaminationRepository;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.ReferralRepository;
import com.nethcare.service.ClinicalService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Referral hand-off and surgeon consultation screens. */
@Controller
public class ReferralWorkflowController {

    private final ClinicalService clinical;
    private final ReferralRepository referrals;
    private final PatientRepository patients;
    private final ExaminationRepository examinations;

    public ReferralWorkflowController(ClinicalService clinical, ReferralRepository referrals,
                                      PatientRepository patients, ExaminationRepository examinations) {
        this.clinical = clinical;
        this.referrals = referrals;
        this.patients = patients;
        this.examinations = examinations;
    }

    @PostMapping("/examinations/{id}/referral")
    public String create(@PathVariable Long id,
                         @RequestParam String surgeon,
                         @RequestParam String reason,
                         @RequestParam(defaultValue = "ROUTINE") String urgency,
                         Authentication authentication,
                         RedirectAttributes redirect) {
        try {
            Referral saved = clinical.refer(id, authentication.getName(), surgeon, reason, urgency);
            redirect.addFlashAttribute("msg", "Referral " + saved.getRefNo() + " sent to the surgeon.");
            return "redirect:/referrals/" + saved.getId();
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/examinations/" + id;
        }
    }

    @GetMapping("/referrals")
    public String worklist(@RequestParam(defaultValue = "PENDING") String status,
                           Authentication authentication, Model model) {
        boolean surgeon = hasRole(authentication, "SURGEON");
        List<Referral> found;
        if (surgeon) {
            found = "all".equalsIgnoreCase(status)
                    ? referrals.findBySurgeonNameOrderByReferredOnDesc(authentication.getName())
                    : referrals.findBySurgeonNameAndStatusOrderByReferredOnDesc(
                            authentication.getName(), normalizedStatus(status));
        } else {
            found = "all".equalsIgnoreCase(status)
                    ? referrals.findAllByOrderByReferredOnDesc()
                    : referrals.findByStatusOrderByReferredOnDesc(normalizedStatus(status));
        }
        model.addAttribute("referrals", found);
        model.addAttribute("status", status);
        model.addAttribute("user", authentication.getName());
        model.addAttribute("surgeonView", surgeon);
        return "clinical/referral-list";
    }

    @GetMapping("/referrals/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        Referral referral = referral(id);
        checkAssignment(referral, authentication);
        model.addAttribute("referral", referral);
        model.addAttribute("patient", patients.findById(referral.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient record is missing.")));
        model.addAttribute("exam", referral.getExaminationId() == null ? null
                : examinations.findById(referral.getExaminationId()).orElse(null));
        model.addAttribute("user", authentication.getName());
        model.addAttribute("canComplete", hasRole(authentication, "SURGEON") || hasRole(authentication, "ADMIN"));
        return "clinical/referral-detail";
    }

    @PostMapping("/referrals/{id}/consultation")
    public String complete(@PathVariable Long id,
                           @RequestParam(required = false) String tests,
                           @RequestParam String diagnosis,
                           @RequestParam String treatment,
                           @RequestParam(required = false) String followUpInstructions,
                           @RequestParam(required = false) String notes,
                           Authentication authentication,
                           RedirectAttributes redirect) {
        try {
            clinical.recordConsultation(id, authentication.getName(), hasRole(authentication, "ADMIN"),
                    tests, diagnosis, treatment, followUpInstructions, notes);
            redirect.addFlashAttribute("msg", "Consultation completed and returned to the patient record.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/referrals/" + id;
    }

    private void checkAssignment(Referral referral, Authentication authentication) {
        if (hasRole(authentication, "SURGEON")
                && !referral.getSurgeonName().equalsIgnoreCase(authentication.getName())) {
            throw new ResourceNotFoundException("No referral with id " + referral.getId());
        }
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private String normalizedStatus(String status) {
        return "COMPLETED".equalsIgnoreCase(status) ? "COMPLETED" : "PENDING";
    }

    private Referral referral(Long id) {
        return referrals.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No referral with id " + id));
    }
}
