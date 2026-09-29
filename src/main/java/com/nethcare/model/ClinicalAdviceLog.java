package com.nethcare.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Entity
@Table(name = "clinical_advice_logs")
@Immutable
public class ClinicalAdviceLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    @Column(name = "clinician_id", nullable = false)
    private Long clinicianId;
    @Column(name = "visit_id", nullable = false)
    private Long visitId;
    @Lob @Column(name = "advice_text", nullable = false)
    private String adviceText;
    @Lob @Column(name = "diagnostic_inputs", nullable = false)
    private String diagnosticInputs;
    @Column(name = "high_risk", nullable = false)
    private boolean highRisk;
    @Column(name = "caution_advice", length = 1000)
    private String cautionAdvice;
    @Column(name = "recorded_at_utc", nullable = false, updatable = false)
    private LocalDateTime recordedAtUtc;
    @Column(name = "previous_hash", length = 64, updatable = false)
    private String previousHash;
    @Column(name = "record_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String recordHash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getClinicianId() { return clinicianId; }
    public void setClinicianId(Long clinicianId) { this.clinicianId = clinicianId; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public String getAdviceText() { return adviceText; }
    public void setAdviceText(String adviceText) { this.adviceText = adviceText; }
    public String getDiagnosticInputs() { return diagnosticInputs; }
    public void setDiagnosticInputs(String diagnosticInputs) { this.diagnosticInputs = diagnosticInputs; }
    public boolean isHighRisk() { return highRisk; }
    public void setHighRisk(boolean highRisk) { this.highRisk = highRisk; }
    public String getCautionAdvice() { return cautionAdvice; }
    public void setCautionAdvice(String cautionAdvice) { this.cautionAdvice = cautionAdvice; }
    public LocalDateTime getRecordedAtUtc() { return recordedAtUtc; }
    public void setRecordedAtUtc(LocalDateTime recordedAtUtc) { this.recordedAtUtc = recordedAtUtc; }
    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }
    public String getRecordHash() { return recordHash; }
    public void setRecordHash(String recordHash) { this.recordHash = recordHash; }
}
