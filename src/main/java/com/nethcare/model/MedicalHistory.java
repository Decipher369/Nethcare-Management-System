package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * An immutable snapshot of the medical history confirmed at a clinical visit.
 * A new row is written for every examination so older clinical decisions keep
 * the history that was known when they were made.
 */
@Entity
@Table(name = "patient_medical_history")
public class MedicalHistory extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "examination_id", nullable = false, unique = true)
    private Long examinationId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "recorded_on", nullable = false)
    private LocalDate recordedOn;

    @Column(name = "recorded_by", nullable = false, length = 100)
    private String recordedBy;

    private boolean diabetic;
    private boolean asthma;
    private boolean hypertension;
    private boolean cardiac;
    private boolean sle;
    private boolean cholesterol;
    private boolean tb;
    private boolean thyroid;
    private boolean arthritis;
    private boolean syphilis;
    private boolean cancer;
    private boolean renal;
    private boolean bronchitis;
    private boolean migraine;

    @Column(name = "ocular_history", length = 1000)
    private String ocularHistory;

    @Column(name = "other_conditions", length = 2000)
    private String otherConditions;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getExaminationId() { return examinationId; }
    public void setExaminationId(Long examinationId) { this.examinationId = examinationId; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
    public LocalDate getRecordedOn() { return recordedOn; }
    public void setRecordedOn(LocalDate recordedOn) { this.recordedOn = recordedOn; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public boolean isDiabetic() { return diabetic; }
    public void setDiabetic(boolean diabetic) { this.diabetic = diabetic; }
    public boolean isAsthma() { return asthma; }
    public void setAsthma(boolean asthma) { this.asthma = asthma; }
    public boolean isHypertension() { return hypertension; }
    public void setHypertension(boolean hypertension) { this.hypertension = hypertension; }
    public boolean isCardiac() { return cardiac; }
    public void setCardiac(boolean cardiac) { this.cardiac = cardiac; }
    public boolean isSle() { return sle; }
    public void setSle(boolean sle) { this.sle = sle; }
    public boolean isCholesterol() { return cholesterol; }
    public void setCholesterol(boolean cholesterol) { this.cholesterol = cholesterol; }
    public boolean isTb() { return tb; }
    public void setTb(boolean tb) { this.tb = tb; }
    public boolean isThyroid() { return thyroid; }
    public void setThyroid(boolean thyroid) { this.thyroid = thyroid; }
    public boolean isArthritis() { return arthritis; }
    public void setArthritis(boolean arthritis) { this.arthritis = arthritis; }
    public boolean isSyphilis() { return syphilis; }
    public void setSyphilis(boolean syphilis) { this.syphilis = syphilis; }
    public boolean isCancer() { return cancer; }
    public void setCancer(boolean cancer) { this.cancer = cancer; }
    public boolean isRenal() { return renal; }
    public void setRenal(boolean renal) { this.renal = renal; }
    public boolean isBronchitis() { return bronchitis; }
    public void setBronchitis(boolean bronchitis) { this.bronchitis = bronchitis; }
    public boolean isMigraine() { return migraine; }
    public void setMigraine(boolean migraine) { this.migraine = migraine; }
    public String getOcularHistory() { return ocularHistory; }
    public void setOcularHistory(String ocularHistory) { this.ocularHistory = ocularHistory; }
    public String getOtherConditions() { return otherConditions; }
    public void setOtherConditions(String otherConditions) { this.otherConditions = otherConditions; }
}
