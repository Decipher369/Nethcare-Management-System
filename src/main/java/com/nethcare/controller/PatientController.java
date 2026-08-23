package com.nethcare.controller;

import com.nethcare.model.Patient;
import com.nethcare.repository.PatientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
    public String list(Model model) {
        model.addAttribute("patients", patients.findAllByOrderByFullNameAsc());
        model.addAttribute("total", patients.count());
        return "patients/list";
    }
}
