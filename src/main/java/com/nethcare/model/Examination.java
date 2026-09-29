package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * One eye test, as taken in the consulting room.
 *
 * Power values are kept as strings on purpose. A prescription of -0.25 and
 * 0.00 and -0.00 all mean "no correction", and storing them as a number makes
 * the prescription comparison show a difference that is not one. Keeping the
 * written form means what the optician wrote is what gets compared.
 *
 * The same values are held for each eye separately because a prescription
 * rarely matches. A single "sphere" column would force the two to be equal.
 */
@Entity
@Table(name = "examinations")
public class Examination extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "examined_by", nullable = false, length = 100)
    private String examinedBy;

    // Right eye / od
    @Column(name = "od_sph", length = 10)
    private String odSph;

    @Column(name = "od_cyl", length = 10)
    private String odCyl;

    @Column(name = "od_axis", length = 10)
    private String odAxis;

    @Column(name = "od_add", length = 10)
    private String odAdd;

    @Column(name = "od_va", length = 10)
    private String odVa;

    // Left eye / os
    @Column(name = "os_sph", length = 10)
    private String osSph;

    @Column(name = "os_cyl", length = 10)
    private String osCyl;

    @Column(name = "os_axis", length = 10)
    private String osAxis;

    @Column(name = "os_add", length = 10)
    private String osAdd;

    @Column(name = "os_va", length = 10)
    private String osVa;

    // Interpupillary distance
    @Column(name = "ipd", length = 10)
    private String ipd;

    // Only for a contact-lens fitting. Left null on a normal sight test.
    @Column(name = "base_curve", length = 10)
    private String baseCurve;

    @Column(name = "diameter", length = 10)
    private String diameter;

    @Column(name = "findings", length = 1000)
    private String findings;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getExaminedBy() { return examinedBy; }
    public void setExaminedBy(String examinedBy) { this.examinedBy = examinedBy; }

    public String getOdSph() { return odSph; }
    public void setOdSph(String odSph) { this.odSph = odSph; }

    public String getOdCyl() { return odCyl; }
    public void setOdCyl(String odCyl) { this.odCyl = odCyl; }

    public String getOdAxis() { return odAxis; }
    public void setOdAxis(String odAxis) { this.odAxis = odAxis; }

    public String getOdAdd() { return odAdd; }
    public void setOdAdd(String odAdd) { this.odAdd = odAdd; }

    public String getOdVa() { return odVa; }
    public void setOdVa(String odVa) { this.odVa = odVa; }

    public String getOsSph() { return osSph; }
    public void setOsSph(String osSph) { this.osSph = osSph; }

    public String getOsCyl() { return osCyl; }
    public void setOsCyl(String osCyl) { this.osCyl = osCyl; }

    public String getOsAxis() { return osAxis; }
    public void setOsAxis(String osAxis) { this.osAxis = osAxis; }

    public String getOsAdd() { return osAdd; }
    public void setOsAdd(String osAdd) { this.osAdd = osAdd; }

    public String getOsVa() { return osVa; }
    public void setOsVa(String osVa) { this.osVa = osVa; }

    public String getIpd() { return ipd; }
    public void setIpd(String ipd) { this.ipd = ipd; }

    public String getBaseCurve() { return baseCurve; }
    public void setBaseCurve(String baseCurve) { this.baseCurve = baseCurve; }

    public String getDiameter() { return diameter; }
    public void setDiameter(String diameter) { this.diameter = diameter; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }
}
