package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.model.*;
import com.nethcare.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class ReportingService {
    public record SalesSummary(LocalDate from, LocalDate to, long billCount, BigDecimal billed,
                               BigDecimal collected, BigDecimal advances, BigDecimal outstanding,
                               Map<String, BigDecimal> byCategory, long orphanPayments) { }
    public record OrderSummary(long total, long open, long overdue, long urgent,
                               Map<OrderStatus, Long> byStatus) { }
    public record StockSummary(long totalItems, long lowCount, BigDecimal stockValue,
                               List<StockItem> lowStock) { }

    private final BillRepository bills;
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final StockItemRepository stock;
    private final FollowUpRepository followUps;
    private final SystemSummaryReportRepository reports;
    private final AuditService audit;

    public ReportingService(BillRepository bills, PaymentRepository payments, OrderRepository orders,
                            StockItemRepository stock, FollowUpRepository followUps,
                            SystemSummaryReportRepository reports, AuditService audit) {
        this.bills = bills; this.payments = payments; this.orders = orders; this.stock = stock;
        this.followUps = followUps; this.reports = reports; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public SalesSummary sales(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BusinessException("A valid report date range is required.");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        List<Bill> rows = bills.findByCreatedAtBetween(start, end).stream()
                .filter(b -> !b.isCancelled() && !b.isCreditNote()).toList();
        BigDecimal billed = sumBills(rows, Bill::getTotal);
        BigDecimal outstanding = rows.stream().map(Bill::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Payment> paid = payments.findByCreatedAtBetween(start, end);
        BigDecimal collected = paid.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal advances = paid.stream().filter(Payment::isAdvance).map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long orphanPayments = paid.stream().filter(payment -> !bills.existsById(payment.getBillId())).count();
        Map<String, BigDecimal> categories = new LinkedHashMap<>();
        for (Bill bill : rows) {
            orders.findById(bill.getOrderId()).ifPresent(order -> {
                for (OrderItem line : order.getItems()) {
                    String category = stock.findById(line.getStockItemId())
                            .map(item -> item.getCategory().label()).orElse("Uncategorised");
                    categories.merge(category, line.lineTotal(), BigDecimal::add);
                }
                BigDecimal adjustment = bill.getTotal().subtract(order.subtotal());
                if (adjustment.compareTo(BigDecimal.ZERO) != 0) {
                    categories.merge("Discounts and surcharges", adjustment, BigDecimal::add);
                }
            });
        }
        return new SalesSummary(from, to, rows.size(), billed, collected, advances, outstanding,
                java.util.Collections.unmodifiableMap(categories), orphanPayments);
    }

    @Transactional(readOnly = true)
    public OrderSummary orders() {
        List<Order> rows = orders.findAll();
        Map<OrderStatus, Long> split = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) split.put(status, 0L);
        rows.forEach(o -> split.compute(o.getStatus(), (k, v) -> v + 1));
        long open = rows.stream().filter(o -> o.getStatus().isOpen()).count();
        long overdue = rows.stream().filter(Order::isOverdue).count();
        long urgent = rows.stream().filter(o -> o.getPriority() == OrderPriority.URGENT).count();
        return new OrderSummary(rows.size(), open, overdue, urgent, split);
    }

    @Transactional(readOnly = true)
    public StockSummary stock() {
        List<StockItem> active = stock.findByIsActiveTrueOrderByCategoryAscNameAsc();
        List<StockItem> low = stock.findLowStock();
        BigDecimal value = active.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StockSummary(active.size(), low.size(), value, low);
    }

    @Transactional
    public SystemSummaryReport saveSalesSnapshot(LocalDate from, LocalDate to,
                                                  Long generatedBy, String actor) {
        SalesSummary s = sales(from, to);
        SystemSummaryReport report = new SystemSummaryReport();
        report.setReportType(ReportType.SALES);
        report.setPeriodStart(from);
        report.setPeriodEnd(to);
        report.setTotalRevenue(s.billed());
        report.setTotalCollected(s.collected());
        report.setTotalOutstanding(s.outstanding());
        report.setTotalOrdersPending(orders().open());
        report.setTotalStockValue(stock().stockValue());
        report.setTotalAttendedPatients(followUps.countByStatusIn(List.of(FollowUpStatus.ATTENDED)));
        report.setBreakdownPayload("{\"bills\":" + s.billCount() + ",\"advances\":"
                + s.advances() + ",\"orphanPayments\":" + s.orphanPayments() + "}");
        report.setGeneratedByStaffId(generatedBy);
        SystemSummaryReport saved = reports.save(report);
        audit.record(actor, AuditAction.CREATE, "SystemSummaryReport", saved.getId().toString(),
                null, "SALES " + from + " to " + to);
        return saved;
    }

    private BigDecimal sumBills(List<Bill> rows, java.util.function.Function<Bill, BigDecimal> value) {
        return rows.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
