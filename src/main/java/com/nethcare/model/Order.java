package com.nethcare.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "order_no", nullable = false, unique = true, length = 30)
    private String orderNo;

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

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }

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

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getPlacedBy() { return placedBy; }
    public void setPlacedBy(String placedBy) { this.placedBy = placedBy; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
