package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Glasses issued from an examination.
 *
 * Nothing here is ever updated. A prescription is a legal document and a wrong
 * one stays on the patient's record — so changing a prescription means issuing
 * a new one from a fresh examination, and the old row is left alone. The exam
 * that produced it is kept so the numbers can be traced back.
 *
 * Prescriptions remain in the clinical record without an automatic expiry.
 */
@Entity
@Table(name = "prescriptions")
public class Prescription extends BaseEntity {

    @Column(name = "rx_no", nullable = false, unique = true, length = 20)
    private String rxNo;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "examination_id", nullable = false)
    private Long examinationId;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "issued_by", nullable = false, length = 100)
    private String issuedBy;

    @Column(name = "od_sph", length = 10)
    private String odSph;

    @Column(name = "od_cyl", length = 10)
    private String odCyl;

    @Column(name = "od_axis", length = 10)
    private String odAxis;

    @Column(name = "od_add", length = 10)
    private String odAdd;

    @Column(name = "os_sph", length = 10)
    private String osSph;

    @Column(name = "os_cyl", length = 10)
    private String osCyl;

    @Column(name = "os_axis", length = 10)
    private String osAxis;

    @Column(name = "os_add", length = 10)
    private String osAdd;

    @Column(name = "ipd", length = 10)
    private String ipd;

    @Column(name = "notes", length = 500)
    private String notes;

    public String getRxNo() { return rxNo; }
    public void setRxNo(String rxNo) { this.rxNo = rxNo; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getExaminationId() { return examinationId; }
    public void setExaminationId(Long examinationId) { this.examinationId = examinationId; }

    public LocalDate getIssuedOn() { return issuedOn; }
    public void setIssuedOn(LocalDate issuedOn) { this.issuedOn = issuedOn; }

    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }

    public String getOdSph() { return odSph; }
    public void setOdSph(String odSph) { this.odSph = odSph; }

    public String getOdCyl() { return odCyl; }
    public void setOdCyl(String odCyl) { this.odCyl = odCyl; }

    public String getOdAxis() { return odAxis; }
    public void setOdAxis(String odAxis) { this.odAxis = odAxis; }

    public String getOdAdd() { return odAdd; }
    public void setOdAdd(String odAdd) { this.odAdd = odAdd; }

    public String getOsSph() { return osSph; }
    public void setOsSph(String osSph) { this.osSph = osSph; }

    public String getOsCyl() { return osCyl; }
    public void setOsCyl(String osCyl) { this.osCyl = osCyl; }

    public String getOsAxis() { return osAxis; }
    public void setOsAxis(String osAxis) { this.osAxis = osAxis; }

    public String getOsAdd() { return osAdd; }
    public void setOsAdd(String osAdd) { this.osAdd = osAdd; }

    public String getIpd() { return ipd; }
    public void setIpd(String ipd) { this.ipd = ipd; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
