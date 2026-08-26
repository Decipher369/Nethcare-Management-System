package com.nethcare.controller;

import com.nethcare.dto.PatientForm;
import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.Patient;
import com.nethcare.repository.PatientRepository;
import com.nethcare.service.PatientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * The patient register — the list the front desk works from.
 */
@Controller
public class PatientController {

    private final PatientRepository patients;
    private final PatientService patientService;

    public PatientController(PatientRepository patients, PatientService patientService) {
        this.patients = patients;
        this.patientService = patientService;
    }

    @GetMapping("/patients")
    public String list(Model model,
                       @RequestParam(name = "q", required = false) String q) {
        List<Patient> found = (q == null || q.isBlank())
                ? patients.findAllByOrderByFullNameAsc()
                : patients.search(q.trim());

        model.addAttribute("patients", found);
        model.addAttribute("total", patients.count());
        model.addAttribute("q", q == null ? "" : q);
        return "patients/list";
    }

    @GetMapping("/patients/new")
    public String newPatientForm(Model model) {
        model.addAttribute("form", new PatientForm());
        return "patients/form";
    }

    @PostMapping("/patients")
    public String register(@ModelAttribute("form") PatientForm form, Model model) {
        try {
            Patient saved = patientService.register(form);
            return "redirect:/patients/" + saved.getId();
        } catch (BusinessException ex) {
            // The global handler answers with JSON, which is no use to someone
            // sitting at the registration form. Send them back with the form
            // still filled in.
            model.addAttribute("form", form);
            model.addAttribute("error", ex.getMessage());
            return "patients/form";
        }
    }

    @GetMapping("/patients/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Patient patient = patients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No patient with id " + id));
        model.addAttribute("patient", patient);
        // Empty until M2 lands — examinations and prescriptions are #25.
        model.addAttribute("visits", List.of());
        return "patients/detail";
    }
}
