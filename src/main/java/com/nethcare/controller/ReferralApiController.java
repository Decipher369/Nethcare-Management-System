package com.nethcare.controller;

import com.nethcare.dto.ApiResponse;
import com.nethcare.dto.ClinicalListDto;
import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.Referral;
import com.nethcare.repository.ReferralRepository;
import com.nethcare.service.ClinicalService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The referral worklists — the endpoints a surgeon and an optician actually
 * open their day with.
 *
 * ClinicalService.referralsFor and ReferralRepository's by-status lookup were
 * both already written and both unreachable: nothing in the application called
 * them. Issue #25 asks for surgeons to see referred patients for 90 days, and
 * that rule needs somewhere to live. Referral.ACCESS_DAYS is the enforcement
 * point, reported per row rather than filtered silently — hiding a closed
 * referral would leave a surgeon wondering why the case vanished.
 */
@RestController
@RequestMapping("/api/referrals")
public class ReferralApiController {

    private final ClinicalService clinical;
    private final ReferralRepository referrals;

    public ReferralApiController(ClinicalService clinical, ReferralRepository referrals) {
        this.clinical = clinical;
        this.referrals = referrals;
    }

    /** Everything referred for one patient, newest first. */
    @GetMapping("/patient/{patientId}")
    public ApiResponse<List<ClinicalListDto.ReferralDto>> forPatient(@PathVariable Long patientId) {
        return ApiResponse.success(clinical.referralsFor(patientId).stream()
                .map(r -> ClinicalListDto.ofReferral(r, LocalDate.now()))
                .toList());
    }

    /**
     * The surgeon's worklist. Defaults to the open cases.
     *
     * status=all returns the closed ones too, since a surgeon chasing an old
     * operation needs to see that it was answered.
     */
    @GetMapping
    public ApiResponse<List<ClinicalListDto.ReferralDto>> worklist(
            @RequestParam(name = "status", defaultValue = "PENDING") String status) {

        List<Referral> found = "all".equalsIgnoreCase(status)
                ? referrals.findAllByOrderByReferredOnDesc()
                : referrals.findByStatusOrderByReferredOnDesc(status.toUpperCase());

        return ApiResponse.success(found.stream()
                .map(r -> ClinicalListDto.ofReferral(r, LocalDate.now()))
                .toList());
    }

    /**
     * Just the ones still inside the 90-day window — the worklist a surgeon
     * can still act on without chasing the clinic.
     */
    @GetMapping("/open")
    public ApiResponse<List<ClinicalListDto.ReferralDto>> stillOpen() {
        LocalDate today = LocalDate.now();
        return ApiResponse.success(referrals.findByStatusOrderByReferredOnDesc("PENDING").stream()
                .filter(r -> r.isOpenForAccess(today))
                .map(r -> ClinicalListDto.ofReferral(r, today))
                .toList());
    }

    /**
     * Surgical notes. The service already refuses a second write; this adds the
     * role check, so only someone who can actually operate gets to say the
     * operation happened.
     */
    @PatchMapping("/{id}/feedback")
    public ApiResponse<Referral> recordFeedback(@PathVariable Long id,
                                                @RequestParam String notes,
                                                Authentication auth) {
        boolean isSurgeon = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SURGEON") || a.getAuthority().equals("ROLE_ADMIN"));
        if (!isSurgeon) {
            throw new BusinessException("Only the surgeon can record feedback on a referral.");
        }
        clinical.recordFeedback(id, notes);
        return ApiResponse.success("Feedback recorded.",
                referrals.findById(id).orElseThrow(() -> new ResourceNotFoundException("No referral with id " + id)));
    }

    /** How the numbers changed between two prescriptions, for the history view. */
    @GetMapping("/compare")
    public ApiResponse<Map<String, String>> compare(@RequestParam Long rx1, @RequestParam Long rx2) {
        return ApiResponse.success(clinical.compareRx(rx1, rx2));
    }
}
