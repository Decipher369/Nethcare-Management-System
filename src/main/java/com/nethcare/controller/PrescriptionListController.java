package com.nethcare.controller;

import com.nethcare.dto.ApiResponse;
import com.nethcare.dto.ClinicalListDto;
import com.nethcare.service.ClinicalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * The prescription history, with each row saying whether it is still valid.
 *
 * A prescription is good for 12 months and after that the patient needs a fresh
 * examination before the counter can order glasses. Prescription.isValidOn
 * already knew that, but nothing called it — a client had to re-derive the
 * 12-month rule from the issue date, which is exactly the sort of thing two
 * parts of the system end up disagreeing about.
 */
@RestController
public class PrescriptionListController {

    private final ClinicalService clinical;

    public PrescriptionListController(ClinicalService clinical) {
        this.clinical = clinical;
    }

    /** Full history for a patient, newest first, each row carrying its own validity. */
    @GetMapping("/api/patients/{id}/prescriptions/validity")
    public ApiResponse<List<ClinicalListDto.PrescriptionDto>> validityFor(@PathVariable Long id) {
        return ApiResponse.success(clinical.prescriptionsFor(id).stream()
                .map(rx -> ClinicalListDto.ofPrescription(rx, LocalDate.now()))
                .toList());
    }

    /** Just the ones that have run out — the list the optician rings people about. */
    @GetMapping("/api/patients/{id}/prescriptions/expired")
    public ApiResponse<List<ClinicalListDto.PrescriptionDto>> expiredFor(@PathVariable Long id) {
        LocalDate today = LocalDate.now();
        return ApiResponse.success(clinical.prescriptionsFor(id).stream()
                .map(rx -> ClinicalListDto.ofPrescription(rx, today))
                .filter(d -> !d.getValid())
                .toList());
    }
}
