package com.nethcare.controller;

import com.nethcare.dto.PatientForm;
import com.nethcare.dto.PatientRegistrationResult;
import com.nethcare.exception.BusinessException;
import com.nethcare.model.Patient;
import com.nethcare.model.Notification;
import com.nethcare.model.NotificationStatus;
import com.nethcare.repository.PatientRepository;
import com.nethcare.service.ClinicalService;
import com.nethcare.service.FollowUpService;
import com.nethcare.service.NotificationDispatchService;
import com.nethcare.service.PatientService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class PatientController {
    private final PatientRepository patients;
    private final PatientService patientService;
    private final ClinicalService clinical;
    private final FollowUpService followUpService;
    private final NotificationDispatchService dispatchService;

    public PatientController(PatientRepository patients, PatientService patientService,
                             ClinicalService clinical, FollowUpService followUpService,
                             NotificationDispatchService dispatchService) {
        this.patients = patients;
        this.patientService = patientService;
        this.clinical = clinical;
        this.followUpService = followUpService;
        this.dispatchService = dispatchService;
    }

    @GetMapping("/patients")
    public String list(Model model, @RequestParam(name = "q", required = false) String q,
                       @RequestParam(name = "status", defaultValue = "active") String status) {
        List<Patient> found;
        if (q != null && !q.isBlank()) found = patients.search(q.trim());
        else if ("inactive".equals(status)) found = patients.findByIsActiveFalseOrderByFullNameAsc();
        else if ("all".equals(status)) found = patients.findAllByOrderByFullNameAsc();
        else found = patients.findByIsActiveTrueOrderByFullNameAsc();
        model.addAttribute("patients", found);
        model.addAttribute("total", found.size());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status);
        return "patients/list";
    }

    @GetMapping("/patients/new")
    public String newPatientForm(Model model) {
        model.addAttribute("form", new PatientForm());
        model.addAttribute("editing", false);
        return "patients/form";
    }

    @PostMapping("/patients")
    public String register(@ModelAttribute("form") PatientForm form, Authentication authentication,
                           Model model, RedirectAttributes redirect) {
        try {
            PatientRegistrationResult result = patientService.register(form, authentication.getName());
            if (result.credential() != null) redirect.addFlashAttribute("credential", result.credential());
            redirect.addFlashAttribute("success", "Patient registered successfully.");
            return "redirect:/patients/" + result.patient().getId();
        } catch (BusinessException ex) {
            model.addAttribute("editing", false);
            model.addAttribute("error", ex.getMessage());
            return "patients/form";
        }
    }

    @GetMapping("/patients/{id:\\d+}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Patient patient = patientService.get(id);
        model.addAttribute("form", toForm(patient));
        model.addAttribute("patient", patient);
        model.addAttribute("editing", true);
        return "patients/form";
    }

    @PostMapping("/patients/{id:\\d+}")
    public String update(@PathVariable Long id, @ModelAttribute("form") PatientForm form,
                         Authentication authentication, Model model, RedirectAttributes redirect) {
        try {
            patientService.update(id, form, authentication.getName());
            redirect.addFlashAttribute("success", "Patient details updated.");
            return "redirect:/patients/" + id;
        } catch (BusinessException ex) {
            model.addAttribute("patient", patientService.get(id));
            model.addAttribute("editing", true);
            model.addAttribute("error", ex.getMessage());
            return "patients/form";
        }
    }

    @PostMapping("/patients/{id}/deactivate")
    public String deactivate(@PathVariable Long id, @RequestParam String reason, Authentication authentication,
                             RedirectAttributes redirect) {
        patientService.deactivate(id, reason, authentication.getName());
        redirect.addFlashAttribute("success", "Patient record deactivated without deleting its history.");
        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/reactivate")
    public String reactivate(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        patientService.reactivate(id, authentication.getName());
        redirect.addFlashAttribute("success", "Patient record reactivated.");
        return "redirect:/patients/" + id;
    }

    @GetMapping("/patients/{id:\\d+}")
    public String detail(@PathVariable Long id, Model model) {
        Patient patient = patientService.get(id);
        model.addAttribute("patient", patient);
        model.addAttribute("visits", clinical.historyFor(id));
        model.addAttribute("prescriptions", clinical.prescriptionsFor(id));
        return "patients/detail";
    }

    @PostMapping("/patients/{id}/send-sms")
    public String sendSms(@PathVariable Long id,
                          @RequestParam(required = false) String message,
                          Authentication authentication,
                          RedirectAttributes redirect) {
        try {
            Notification n = followUpService.sendPatientSms(id, message, authentication.getName());
            if (n.getStatus() == NotificationStatus.FAILED) {
                redirect.addFlashAttribute("error", "SMS cannot be sent: " + n.getFailureReason());
                return "redirect:/patients/" + id;
            }
            boolean sent = dispatchService.dispatchSingle(n, authentication.getName());
            if (sent) {
                redirect.addFlashAttribute("success",
                        "SMS dispatched successfully to " + n.getDestination() + " (Receipt: " + n.getGatewayReceiptId() + ")");
            } else {
                redirect.addFlashAttribute("success", "SMS queued for next dispatch (Reference: " + n.getReference() + ")");
            }
            return "redirect:/patients/" + id;
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "Failed to dispatch SMS: " + ex.getMessage());
            return "redirect:/patients/" + id;
        }
    }

    private PatientForm toForm(Patient patient) {
        PatientForm form = new PatientForm();
        form.setFullName(patient.getFullName()); form.setNic(patient.getNic());
        form.setDob(patient.getDob().toString()); form.setGender(patient.getGender());
        form.setPhone(patient.getPhone()); form.setEmail(patient.getEmail());
        form.setAddress(patient.getAddress()); form.setBloodGroup(patient.getBloodGroup());
        form.setGuardianName(patient.getGuardianName()); form.setGuardianPhone(patient.getGuardianPhone());
        form.setRegistrationNotes(patient.getRegistrationNotes());
        form.setConsentGiven(patient.isConsentGiven());
        return form;
    }
}
