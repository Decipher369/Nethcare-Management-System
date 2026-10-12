package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.model.*;
import com.nethcare.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock private BillRepository bills;
    @Mock private PaymentRepository payments;
    @Mock private OrderRepository orders;
    @Mock private StockItemRepository stock;
    @Mock private FollowUpRepository followUps;
    @Mock private SystemSummaryReportRepository reports;
    @Mock private AuditService audit;

    private ReportingService service;

    @BeforeEach
    void setUp() {
        service = new ReportingService(bills, payments, orders, stock, followUps, reports, audit);
    }

    @Test
    void salesRejectsInvalidDateRange() {
        assertThrows(BusinessException.class, () -> service.sales(null, LocalDate.now()));
        assertThrows(BusinessException.class, () -> service.sales(LocalDate.now(), null));
        assertThrows(BusinessException.class, () -> service.sales(LocalDate.now().plusDays(1), LocalDate.now()));
    }

    @Test
    void salesReturnsZeroTotalsWhenNoTransactions() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        when(bills.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(payments.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        ReportingService.SalesSummary summary = service.sales(from, to);

        assertNotNull(summary);
        assertEquals(0, summary.billCount());
        assertEquals(BigDecimal.ZERO, summary.billed());
        assertEquals(BigDecimal.ZERO, summary.collected());
        assertEquals(BigDecimal.ZERO, summary.advances());
        assertEquals(BigDecimal.ZERO, summary.outstanding());
        assertTrue(summary.byCategory().isEmpty());
        assertEquals(0, summary.orphanPayments());
    }

    @Test
    void salesAggregatesBilledCollectedAndOutstanding() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();

        Bill bill = new Bill();
        bill.setId(101L);
        bill.setOrderId(201L);
        bill.setTotal(new BigDecimal("15000.00"));
        bill.setPaid(new BigDecimal("5000.00"));
        bill.setCancelled(false);
        bill.setCreditNote(false);

        Payment p1 = new Payment();
        p1.setId(1L);
        p1.setBillId(101L);
        p1.setAmount(new BigDecimal("5000.00"));
        p1.setAdvance(true);

        when(bills.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(bill));
        when(payments.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(p1));
        when(bills.existsById(101L)).thenReturn(true);

        Order order = new Order();
        order.setId(201L);
        OrderItem item = new OrderItem();
        item.setStockItemId(501L);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("15000.00"));
        order.setItems(List.of(item));

        when(orders.findById(201L)).thenReturn(Optional.of(order));

        StockItem stockItem = new StockItem();
        stockItem.setId(501L);
        stockItem.setCategory(StockCategory.FRAME);
        when(stock.findById(501L)).thenReturn(Optional.of(stockItem));

        ReportingService.SalesSummary summary = service.sales(from, to);

        assertEquals(1, summary.billCount());
        assertEquals(new BigDecimal("15000.00"), summary.billed());
        assertEquals(new BigDecimal("5000.00"), summary.collected());
        assertEquals(new BigDecimal("5000.00"), summary.advances());
        assertEquals(new BigDecimal("10000.00"), summary.outstanding());
        assertTrue(summary.byCategory().containsKey(StockCategory.FRAME.label()));
    }

    @Test
    void orderSummaryCountsCorrectly() {
        Order o1 = new Order();
        o1.setStatus(OrderStatus.PLACED);
        o1.setPriority(OrderPriority.URGENT);

        Order o2 = new Order();
        o2.setStatus(OrderStatus.COLLECTED);
        o2.setPriority(OrderPriority.NORMAL);

        when(orders.findAll()).thenReturn(List.of(o1, o2));

        ReportingService.OrderSummary summary = service.orders();

        assertEquals(2, summary.total());
        assertEquals(1, summary.open());
        assertEquals(1, summary.urgent());
        assertEquals(1L, summary.byStatus().get(OrderStatus.PLACED));
        assertEquals(1L, summary.byStatus().get(OrderStatus.COLLECTED));
    }

    @Test
    void stockSummaryCalculatesTotals() {
        StockItem s1 = new StockItem();
        s1.setId(1L);
        s1.setUnitPrice(new BigDecimal("2000.00"));
        s1.setQuantity(5);
        s1.setIsActive(true);

        StockItem s2 = new StockItem();
        s2.setId(2L);
        s2.setUnitPrice(new BigDecimal("1500.00"));
        s2.setQuantity(2);
        s2.setReorderLevel(5);
        s2.setIsActive(true);

        when(stock.findByIsActiveTrueOrderByCategoryAscNameAsc()).thenReturn(List.of(s1, s2));
        when(stock.findLowStock()).thenReturn(List.of(s2));

        ReportingService.StockSummary summary = service.stock();

        assertEquals(2, summary.totalItems());
        assertEquals(1, summary.lowCount());
        assertEquals(new BigDecimal("13000.00"), summary.stockValue());
    }

    @Test
    void saveSalesSnapshotPersistsAndAudits() {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        when(bills.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(payments.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(orders.findAll()).thenReturn(List.of());
        when(stock.findByIsActiveTrueOrderByCategoryAscNameAsc()).thenReturn(List.of());
        when(stock.findLowStock()).thenReturn(List.of());
        when(followUps.countByStatusIn(any())).thenReturn(15L);

        when(reports.save(any(SystemSummaryReport.class))).thenAnswer(invocation -> {
            SystemSummaryReport r = invocation.getArgument(0);
            r.setId(99L);
            return r;
        });

        SystemSummaryReport saved = service.saveSalesSnapshot(from, to, 1L, "admin");

        assertNotNull(saved);
        assertEquals(99L, saved.getId());
        assertEquals(ReportType.SALES, saved.getReportType());
        assertEquals(from, saved.getPeriodStart());
        assertEquals(to, saved.getPeriodEnd());
        assertEquals(15L, saved.getTotalAttendedPatients());
        verify(reports).save(any(SystemSummaryReport.class));
        verify(audit).record(eq("admin"), eq(AuditAction.CREATE), eq("SystemSummaryReport"), eq("99"), isNull(), anyString());
    }
}
