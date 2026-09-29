package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.model.*;
import com.nethcare.repository.ClinicalAdviceLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.time.temporal.ChronoUnit;

@Service
public class ClinicalAdviceService {
    private final ClinicalAdviceLogRepository logs;
    private final AuditService audit;

    public ClinicalAdviceService(ClinicalAdviceLogRepository logs, AuditService audit) {
        this.logs = logs;
        this.audit = audit;
    }

    @Transactional
    public ClinicalAdviceLog append(Long patientId, Long clinicianId, Long visitId,
                                    String advice, String diagnosticInputs,
                                    boolean highRisk, String cautionAdvice, String actor) {
        if (patientId == null || clinicianId == null || visitId == null
                || blank(advice) || blank(diagnosticInputs)) {
            throw new BusinessException("Patient, clinician, visit, advice and diagnostic inputs are required.");
        }
        if (highRisk && blank(cautionAdvice)) {
            throw new BusinessException("Caution advice is required for a high-risk condition.");
        }
        ClinicalAdviceLog row = new ClinicalAdviceLog();
        row.setPatientId(patientId);
        row.setClinicianId(clinicianId);
        row.setVisitId(visitId);
        row.setAdviceText(advice.trim());
        row.setDiagnosticInputs(diagnosticInputs.trim());
        row.setHighRisk(highRisk);
        row.setCautionAdvice(blank(cautionAdvice) ? null : cautionAdvice.trim());
        row.setRecordedAtUtc(LocalDateTime.now(Clock.systemUTC()).truncatedTo(ChronoUnit.MICROS));
        String previous = logs.findTopByOrderByIdDesc().map(ClinicalAdviceLog::getRecordHash).orElse(null);
        row.setPreviousHash(previous);
        row.setRecordHash(hash(previous, row));
        ClinicalAdviceLog saved = logs.save(row);
        audit.record(actor, AuditAction.CREATE, "ClinicalAdviceLog", String.valueOf(saved.getId()),
                null, saved.getRecordHash(), "Tamper-evident clinical advice recorded");
        return saved;
    }

    private String hash(String previous, ClinicalAdviceLog row) {
        String value = String.join("|", safe(previous), row.getPatientId().toString(),
                row.getClinicianId().toString(), row.getVisitId().toString(), row.getAdviceText(),
                row.getDiagnosticInputs(), Boolean.toString(row.isHighRisk()),
                safe(row.getCautionAdvice()), row.getRecordedAtUtc().toString());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String safe(String value) { return value == null ? "" : value; }
}
