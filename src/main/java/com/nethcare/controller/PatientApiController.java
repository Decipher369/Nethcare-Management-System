package com.nethcare.controller;

import com.nethcare.dto.ApiResponse;
import com.nethcare.dto.PatientDto;
import com.nethcare.dto.PatientForm;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.Patient;
import com.nethcare.repository.PatientRepository;
import com.nethcare.service.PatientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The patient register over JSON — the same data as PatientController's HTML
 * pages, for anything that is not a browser form.
 *
 * Registration calls PatientService rather than duplicating its rules, so the
 * form and the API cannot drift apart on what counts as a valid patient.
 */
@RestController
@RequestMapping("/api/patients")
public class PatientApiController {

    private final PatientRepository patients;
    private final PatientService patientService;

    public PatientApiController(PatientRepository patients, PatientService patientService) {
        this.patients = patients;
        this.patientService = patientService;
    }

    @GetMapping
    public ApiResponse<List<PatientDto>> list(@RequestParam(name = "q", required = false) String q) {
        List<Patient> found = (q == null || q.isBlank())
                ? patients.findAllByOrderByFullNameAsc()
                : patients.search(q.trim());
        return ApiResponse.success(PatientDto.of(found));
    }

    @GetMapping("/{id}")
    public ApiResponse<PatientDto> get(@PathVariable Long id) {
        return ApiResponse.success(PatientDto.of(load(id)));
    }

    @PostMapping
    public ApiResponse<PatientDto> register(@RequestBody PatientForm form) {
        return ApiResponse.success("Patient registered.", PatientDto.of(patientService.register(form)));
    }

    // Only the fields a person can correct go here. patient_no, user_id and
    // registered_on are assigned at creation and stay put, so a PUT cannot
    // quietly renumber a patient or repoint their portal login.
    @PutMapping("/{id}")
    public ApiResponse<PatientDto> update(@PathVariable Long id, @RequestBody PatientForm form) {
        Patient p = load(id);

        if (form.getFullName() != null && !form.getFullName().isBlank()) {
            p.setFullName(form.getFullName().trim());
        }
        if (form.getDob() != null && !form.getDob().isBlank()) {
            p.setDob(java.time.LocalDate.parse(form.getDob()));
        }
        if (form.getGender() != null) {
            p.setGender(blankToNull(form.getGender()));
        }
        if (form.getPhone() != null) {
            p.setPhone(blankToNull(form.getPhone()));
        }
        if (form.getEmail() != null) {
            p.setEmail(blankToNull(form.getEmail()));
        }
        if (form.getAddress() != null) {
            p.setAddress(blankToNull(form.getAddress()));
        }
        if (form.getBloodGroup() != null) {
            p.setBloodGroup(blankToNull(form.getBloodGroup()));
        }
        return ApiResponse.success("Patient updated.", PatientDto.of(patients.save(p)));
    }

    // Visits belong to M2 and are not on this branch yet, so history comes back
    // empty rather than pretending. #25 fills it in.
    @GetMapping("/{id}/history")
    public ApiResponse<Map<String, Object>> history(@PathVariable Long id) {
        Patient p = load(id);
        return ApiResponse.success(Map.of(
                "patient", PatientDto.of(p),
                "visits", List.of()
        ));
    }

    private Patient load(Long id) {
        return patients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No patient with id " + id));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
