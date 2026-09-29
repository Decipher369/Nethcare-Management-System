package com.nethcare.controller;

import com.nethcare.dto.*;
import com.nethcare.model.Patient;
import com.nethcare.repository.PatientRepository;
import com.nethcare.service.ClinicalService;
import com.nethcare.service.PatientService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class PatientApiController {
    private final PatientRepository patients;
    private final PatientService patientService;
    private final ClinicalService clinical;

    public PatientApiController(PatientRepository patients, PatientService patientService, ClinicalService clinical) {
        this.patients = patients;
        this.patientService = patientService;
        this.clinical = clinical;
    }

    @GetMapping
    public ApiResponse<List<PatientDto>> list(@RequestParam(name = "q", required = false) String q) {
        List<Patient> found = q == null || q.isBlank()
                ? patients.findByIsActiveTrueOrderByFullNameAsc() : patients.search(q.trim());
        return ApiResponse.success(PatientDto.of(found));
    }

    @GetMapping("/{id}")
    public ApiResponse<PatientDto> get(@PathVariable Long id) {
        return ApiResponse.success(PatientDto.of(patientService.get(id)));
    }

    @PostMapping
    public ApiResponse<PatientRegistrationResponse> register(@RequestBody PatientForm form, Authentication authentication) {
        return ApiResponse.success("Patient registered.",
                PatientRegistrationResponse.of(patientService.register(form, authentication.getName())));
    }

    @PutMapping("/{id}")
    public ApiResponse<PatientDto> update(@PathVariable Long id, @RequestBody PatientForm form,
                                          Authentication authentication) {
        return ApiResponse.success("Patient updated.", PatientDto.of(patientService.update(id, form, authentication.getName())));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<Map<String, Object>> history(@PathVariable Long id) {
        Patient patient = patientService.get(id);
        return ApiResponse.success(Map.of("patient", PatientDto.of(patient),
                "visits", clinical.historyFor(id), "prescriptions", clinical.prescriptionsFor(id)));
    }
}
