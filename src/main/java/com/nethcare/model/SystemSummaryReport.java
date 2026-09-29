package com.nethcare.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "system_summary_reports")
public class SystemSummaryReport extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType reportType;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    @Column(name = "total_collected", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCollected = BigDecimal.ZERO;
    @Column(name = "total_outstanding", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalOutstanding = BigDecimal.ZERO;
    @Column(name = "total_attended_patients", nullable = false)
    private long totalAttendedPatients;
    @Column(name = "total_orders_pending", nullable = false)
    private long totalOrdersPending;
    @Column(name = "total_stock_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalStockValue = BigDecimal.ZERO;
    @Lob @Column(name = "breakdown_payload")
    private String breakdownPayload;
    @Column(name = "generated_by_staff_id", nullable = false)
    private Long generatedByStaffId;

    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public BigDecimal getTotalCollected() { return totalCollected; }
    public void setTotalCollected(BigDecimal totalCollected) { this.totalCollected = totalCollected; }
    public BigDecimal getTotalOutstanding() { return totalOutstanding; }
    public void setTotalOutstanding(BigDecimal totalOutstanding) { this.totalOutstanding = totalOutstanding; }
    public long getTotalAttendedPatients() { return totalAttendedPatients; }
    public void setTotalAttendedPatients(long totalAttendedPatients) { this.totalAttendedPatients = totalAttendedPatients; }
    public long getTotalOrdersPending() { return totalOrdersPending; }
    public void setTotalOrdersPending(long totalOrdersPending) { this.totalOrdersPending = totalOrdersPending; }
    public BigDecimal getTotalStockValue() { return totalStockValue; }
    public void setTotalStockValue(BigDecimal totalStockValue) { this.totalStockValue = totalStockValue; }
    public String getBreakdownPayload() { return breakdownPayload; }
    public void setBreakdownPayload(String breakdownPayload) { this.breakdownPayload = breakdownPayload; }
    public Long getGeneratedByStaffId() { return generatedByStaffId; }
    public void setGeneratedByStaffId(Long generatedByStaffId) { this.generatedByStaffId = generatedByStaffId; }
}
