package com.nethcare.controller;

import com.nethcare.model.Patient;
import com.nethcare.repository.PatientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * The patient register — the list the front desk works from.
 */
@Controller
public class PatientController {

    private final PatientRepository patients;

    public PatientController(PatientRepository patients) {
        this.patients = patients;
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
}
