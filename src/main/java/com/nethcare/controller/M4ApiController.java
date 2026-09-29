package com.nethcare.controller;

import com.nethcare.dto.ApiResponse;
import com.nethcare.model.*;
import com.nethcare.service.ClinicalAdviceService;
import com.nethcare.service.FollowUpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class M4ApiController {
    private final FollowUpService followUps;
    private final ClinicalAdviceService advice;

    public M4ApiController(FollowUpService followUps, ClinicalAdviceService advice) {
        this.followUps = followUps; this.advice = advice;
    }

    public record FollowUpRequest(@NotNull Long patientId, @NotNull Long assignedOpticianId,
                                  @NotNull Long originatingVisitId, @NotNull FollowUpCategory category,
                                  @NotNull LocalDate targetReviewDate, boolean highRisk,
                                  String clinicalNotes) { }

    public record AdviceRequest(@NotNull Long patientId, @NotNull Long clinicianId,
                                @NotNull Long visitId, @NotBlank String advice,
                                @NotBlank String diagnosticInputs, boolean highRisk,
                                String cautionAdvice) { }

    public record DispatchReceipt(@NotBlank String gatewayReceiptId) { }
    public record DispatchFailure(@NotBlank String reason, boolean retryable) { }

    @PostMapping("/followups")
    public ApiResponse<FollowUp> create(@Valid @RequestBody FollowUpRequest r, Authentication auth) {
        return ApiResponse.success(followUps.createCase(r.patientId(), r.assignedOpticianId(),
                r.originatingVisitId(), r.category(), r.targetReviewDate(), r.highRisk(),
                r.clinicalNotes(), auth.getName()));
    }

    @GetMapping("/followups/due")
    public ApiResponse<List<FollowUp>> due(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(followUps.weeklyList(from, to));
    }

    @PostMapping("/followups/{id}/notifications")
    public ApiResponse<Notification> queue(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success(followUps.notify(id, auth.getName()));
    }

    @PostMapping("/followups/notifications/queue")
    public ApiResponse<Integer> queueCohort(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication auth) {
        return ApiResponse.success(followUps.queueCohort(from, to, auth.getName()));
    }

    @PostMapping("/followups/notifications/{id}/sent")
    public ApiResponse<Notification> sent(@PathVariable Long id, @Valid @RequestBody DispatchReceipt r,
                                          Authentication auth) {
        return ApiResponse.success(followUps.markSent(id, r.gatewayReceiptId(), auth.getName()));
    }

    @PostMapping("/followups/notifications/{id}/failed")
    public ApiResponse<Notification> failed(@PathVariable Long id, @Valid @RequestBody DispatchFailure r,
                                            Authentication auth) {
        return ApiResponse.success(followUps.markFailed(id, r.reason(), r.retryable(), auth.getName()));
    }

    @PostMapping("/followups/notifications/{id}/delivered")
    public ApiResponse<Notification> delivered(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success(followUps.markDelivered(id, auth.getName()));
    }

    @PostMapping("/clinical-advice")
    public ApiResponse<ClinicalAdviceLog> advice(@Valid @RequestBody AdviceRequest r, Authentication auth) {
        return ApiResponse.success(advice.append(r.patientId(), r.clinicianId(), r.visitId(), r.advice(),
                r.diagnosticInputs(), r.highRisk(), r.cautionAdvice(), auth.getName()));
    }
}
