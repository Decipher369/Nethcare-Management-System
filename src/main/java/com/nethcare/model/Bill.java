package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A bill raised against an order — INV-1042 on the client's sketch.
 *
 * The totals are stored, not derived at read time. A bill is a document the
 * customer keeps; if the price list changes next year, reprinting an old bill
 * must still show what they actually owed. So subtotal, discount, surcharge and
 * total are fixed when the bill is generated.
 *
 * The bill number carries an "INV-" prefix as the client asked.
 */
@Entity
@Table(name = "bills")
public class Bill extends BaseEntity {

    @Column(name = "bill_no", nullable = false, unique = true, length = 30)
    private String billNo;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    /** Added on top for an urgent order (FR-3.3). */
    @Column(name = "surcharge", nullable = false, precision = 12, scale = 2)
    private BigDecimal surcharge = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal paid = BigDecimal.ZERO;

    /**
     * Follow-up date printed on the bill (FR-4.1) — when the customer should
     * come back for a check.
     */
    @Column(name = "follow_up_on")
    private java.time.LocalDate followUpOn;

    @Column(name = "cancelled", nullable = false)
    private boolean cancelled = false;

    /**
     * Raised when an order is cancelled and money was taken. A credit note is
     * the refund paperwork; it stays on file rather than being deleted, because
     * the money genuinely moved and M4 reports on it.
     */
    @Column(name = "is_credit_note", nullable = false)
    private boolean creditNote = false;

    @Column(name = "credit_note_no", length = 30)
    private String creditNoteNo;

    /** What is still owed — zero once the order is collected. */
    public BigDecimal balance() {
        BigDecimal due = total.subtract(paid);
        return due.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : due;
    }

    public boolean isSettled() {
        return balance().compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Whether the customer has paid enough to send the order to the lab. The
     * percentage comes from configuration, not a number typed into the bill.
     */
    public boolean meetsAdvance(BigDecimal requiredPercent) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        return paid.multiply(BigDecimal.valueOf(100))
                .compareTo(total.multiply(requiredPercent)) >= 0;
    }

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getSurcharge() { return surcharge; }
    public void setSurcharge(BigDecimal surcharge) { this.surcharge = surcharge; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal getPaid() { return paid; }
    public void setPaid(BigDecimal paid) { this.paid = paid; }

    public java.time.LocalDate getFollowUpOn() { return followUpOn; }
    public void setFollowUpOn(java.time.LocalDate followUpOn) { this.followUpOn = followUpOn; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public boolean isCreditNote() { return creditNote; }
    public void setCreditNote(boolean creditNote) { this.creditNote = creditNote; }

    public String getCreditNoteNo() { return creditNoteNo; }
    public void setCreditNoteNo(String creditNoteNo) { this.creditNoteNo = creditNoteNo; }
}
