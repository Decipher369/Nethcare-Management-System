package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One payment taken at the counter, against a bill.
 *
 * Append-only. A bill's running total comes from summing these rather than
 * from a column that gets overwritten, so the advance and the final balance
 * are separate entries and the history stays intact for M4's reports.
 *
 * Staff record these by hand. There is no gateway, no card number, and
 * nothing the customer types in.
 */
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    /** Receipt number handed to the customer, e.g. RCP-0031. */
    @Column(name = "receipt_no", nullable = false, length = 30)
    private String receiptNo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method = PaymentMethod.CASH;

    /**
     * True for the deposit taken before the order goes to the lab, false for
     * the balance on collection.
     */
    @Column(name = "is_advance", nullable = false)
    private boolean advance = false;

    @Column(name = "taken_by", length = 80)
    private String takenBy;

    @Column(length = 200)
    private String note;

    public Payment() {
    }

    public Payment(Long billId, String receiptNo, BigDecimal amount,
                   PaymentMethod method, boolean advance, String takenBy) {
        this.billId = billId;
        this.receiptNo = receiptNo;
        this.amount = amount;
        this.method = method;
        this.advance = advance;
        this.takenBy = takenBy;
    }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public boolean isAdvance() { return advance; }
    public void setAdvance(boolean advance) { this.advance = advance; }

    public String getTakenBy() { return takenBy; }
    public void setTakenBy(String takenBy) { this.takenBy = takenBy; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
