package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Symptoms reported for one examination. */
@Entity
@Table(name = "clinical_symptoms")
public class ClinicalSymptom extends BaseEntity {

    @Column(name = "examination_id", nullable = false, unique = true)
    private Long examinationId;

    private boolean headache;
    private boolean blurVision;
    private boolean tearing;
    private boolean redness;
    private boolean itching;
    private boolean strain;
    private boolean floaters;
    private boolean flashes;
    private boolean burning;
    private boolean discharge;
    private boolean doubleVision;
    private boolean squint;
    private boolean glare;
    private boolean pain;
    private boolean distortion;
    private boolean dry;
    private boolean photophobia;

    @Column(name = "additional_symptoms", length = 2000)
    private String additionalSymptoms;

    public Long getExaminationId() { return examinationId; }
    public void setExaminationId(Long examinationId) { this.examinationId = examinationId; }
    public boolean isHeadache() { return headache; }
    public void setHeadache(boolean headache) { this.headache = headache; }
    public boolean isBlurVision() { return blurVision; }
    public void setBlurVision(boolean blurVision) { this.blurVision = blurVision; }
    public boolean isTearing() { return tearing; }
    public void setTearing(boolean tearing) { this.tearing = tearing; }
    public boolean isRedness() { return redness; }
    public void setRedness(boolean redness) { this.redness = redness; }
    public boolean isItching() { return itching; }
    public void setItching(boolean itching) { this.itching = itching; }
    public boolean isStrain() { return strain; }
    public void setStrain(boolean strain) { this.strain = strain; }
    public boolean isFloaters() { return floaters; }
    public void setFloaters(boolean floaters) { this.floaters = floaters; }
    public boolean isFlashes() { return flashes; }
    public void setFlashes(boolean flashes) { this.flashes = flashes; }
    public boolean isBurning() { return burning; }
    public void setBurning(boolean burning) { this.burning = burning; }
    public boolean isDischarge() { return discharge; }
    public void setDischarge(boolean discharge) { this.discharge = discharge; }
    public boolean isDoubleVision() { return doubleVision; }
    public void setDoubleVision(boolean doubleVision) { this.doubleVision = doubleVision; }
    public boolean isSquint() { return squint; }
    public void setSquint(boolean squint) { this.squint = squint; }
    public boolean isGlare() { return glare; }
    public void setGlare(boolean glare) { this.glare = glare; }
    public boolean isPain() { return pain; }
    public void setPain(boolean pain) { this.pain = pain; }
    public boolean isDistortion() { return distortion; }
    public void setDistortion(boolean distortion) { this.distortion = distortion; }
    public boolean isDry() { return dry; }
    public void setDry(boolean dry) { this.dry = dry; }
    public boolean isPhotophobia() { return photophobia; }
    public void setPhotophobia(boolean photophobia) { this.photophobia = photophobia; }
    public String getAdditionalSymptoms() { return additionalSymptoms; }
    public void setAdditionalSymptoms(String additionalSymptoms) { this.additionalSymptoms = additionalSymptoms; }
}
