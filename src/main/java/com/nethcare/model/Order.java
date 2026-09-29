package com.nethcare.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A spectacle order: what the customer asked for, and where it has got to.
 *
 * Built from a prescription, so it carries the lens specification copied in.
 * The copy is deliberate — the prescription may be reissued next year, but the
 * glasses already made to last spec must not change with it.
 *
 * customerName and customerPhone are here instead of a patient id on purpose.
 * Module 3 branches from main, where no patient table exists yet; the link to
 * the M1 Patient is added when the modules merge.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_patient", columnList = "patient_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_prescription", columnList = "prescription_id")
})
public class Order extends BaseEntity {

    @Column(name = "order_no", nullable = false, unique = true, length = 30)
    private String orderNo;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", insertable = false, updatable = false)
    private Patient patient;

    @Column(name = "examination_id")
    private Long examinationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examination_id", insertable = false, updatable = false)
    private Examination examination;

    @Column(name = "patient_no_snapshot", nullable = false, length = 20)
    private String patientNoSnapshot;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    /**
     * The prescription this was made from. Plain id for now, matching the
     * reasoning above.
     */
    @Column(name = "prescription_id")
    private Long prescriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", insertable = false, updatable = false)
    private Prescription prescription;

    @Column(name = "prescription_no_snapshot", length = 30)
    private String prescriptionNoSnapshot;

    @Column(name = "frame_id")
    private Long frameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "frame_id", insertable = false, updatable = false)
    private Frame frame;

    @Column(name = "lens_id")
    private Long lensId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lens_id", insertable = false, updatable = false)
    private Lens lens;

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
    @Column(name = "pupillary_distance", length = 10)
    private String pupillaryDistance;

    @Column(name = "lens_type", length = 30)
    private String lensType;

    @Column(length = 60)
    private String coating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderPriority priority = OrderPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PLACED;

    /** The date the shop promised the customer. */
    @Column(name = "promised_on")
    private LocalDate promisedOn;

    @Column(name = "ordered_on", nullable = false)
    private LocalDate orderedOn;

    @Column(name = "ready_on")
    private LocalDate readyOn;

    @Column(name = "collected_on")
    private LocalDate collectedOn;

    @Column(name = "collected_by", length = 80)
    private String collectedBy;

    @Column(length = 500)
    private String remarks;

    @Column(name = "placed_by", length = 80)
    private String placedBy;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    @OrderBy("id asc")
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    /**
     * Sum of the item lines at the price they were ordered at. Discounts and
     * the urgent surcharge belong to the bill, not here — the order records
     * what was bought, the bill records what was charged.
     */
    public BigDecimal subtotal() {
        return items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Past the promised date and still not collected.
     *
     * On the entity rather than in the template: Thymeleaf 3.1 removed
     * unrestricted T() calls, so a view cannot ask for LocalDate.now() without
     * being handed the date first. An order asked to judge itself is also
     * easier to reuse from the overdue filter.
     */
    public boolean isOverdue() {
        return promisedOn != null
                && promisedOn.isBefore(LocalDate.now())
                && status.isOpen();
    }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getExaminationId() { return examinationId; }
    public void setExaminationId(Long examinationId) { this.examinationId = examinationId; }
    public String getPatientNoSnapshot() { return patientNoSnapshot; }
    public void setPatientNoSnapshot(String patientNoSnapshot) { this.patientNoSnapshot = patientNoSnapshot; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public String getPrescriptionNoSnapshot() { return prescriptionNoSnapshot; }
    public void setPrescriptionNoSnapshot(String prescriptionNoSnapshot) { this.prescriptionNoSnapshot = prescriptionNoSnapshot; }
    public Long getFrameId() { return frameId; }
    public void setFrameId(Long frameId) { this.frameId = frameId; }
    public Long getLensId() { return lensId; }
    public void setLensId(Long lensId) { this.lensId = lensId; }
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
    public String getPupillaryDistance() { return pupillaryDistance; }
    public void setPupillaryDistance(String pupillaryDistance) { this.pupillaryDistance = pupillaryDistance; }

    public String getLensType() { return lensType; }
    public void setLensType(String lensType) { this.lensType = lensType; }

    public String getCoating() { return coating; }
    public void setCoating(String coating) { this.coating = coating; }

    public OrderPriority getPriority() { return priority; }
    public void setPriority(OrderPriority priority) { this.priority = priority; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public LocalDate getPromisedOn() { return promisedOn; }
    public void setPromisedOn(LocalDate promisedOn) { this.promisedOn = promisedOn; }
    public LocalDate getOrderedOn() { return orderedOn; }
    public void setOrderedOn(LocalDate orderedOn) { this.orderedOn = orderedOn; }
    public LocalDate getReadyOn() { return readyOn; }
    public void setReadyOn(LocalDate readyOn) { this.readyOn = readyOn; }
    public LocalDate getCollectedOn() { return collectedOn; }
    public void setCollectedOn(LocalDate collectedOn) { this.collectedOn = collectedOn; }
    public String getCollectedBy() { return collectedBy; }
    public void setCollectedBy(String collectedBy) { this.collectedBy = collectedBy; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getPlacedBy() { return placedBy; }
    public void setPlacedBy(String placedBy) { this.placedBy = placedBy; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
