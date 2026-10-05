package com.nethcare.controller;

import com.nethcare.model.*;
import com.nethcare.repository.*;
import com.nethcare.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class ReportController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private final FollowUpService followUpService;
    private final FollowUpRepository followUps;
    private final NotificationRepository notifications;
    private final AuditLogRepository auditLogs;
    private final AuditService auditService;
    private final ReportingService reporting;
    private final UserRepository users;

    public ReportController(FollowUpService followUpService, FollowUpRepository followUps,
                            NotificationRepository notifications, AuditLogRepository auditLogs,
                            AuditService auditService, ReportingService reporting, UserRepository users) {
        this.followUpService = followUpService; this.followUps = followUps;
        this.notifications = notifications; this.auditLogs = auditLogs;
        this.auditService = auditService; this.reporting = reporting; this.users = users;
    }

    @GetMapping("/dashboard/console")
    public String dashboard(Authentication auth, Model model) {
        who(auth, model, "Dashboard", "dashboard");
        ReportingService.SalesSummary sales = reporting.sales(YearMonth.now().atDay(1), LocalDate.now());
        ReportingService.OrderSummary orders = reporting.orders();
        ReportingService.StockSummary stock = reporting.stock();
        model.addAttribute("salesThisMonth", money(sales.billed()));
        model.addAttribute("patientsAttended", followUps.countByStatusIn(List.of(FollowUpStatus.ATTENDED)));
        model.addAttribute("ordersCompleted", orders.byStatus().get(OrderStatus.COLLECTED));
        model.addAttribute("lowStockCount", stock.lowCount());
        model.addAttribute("trend", salesTrend());
        model.addAttribute("orderSplit", orderRows(orders));
        model.addAttribute("totalOrders", orders.total());
        model.addAttribute("lowStock", stockRows(stock.lowStock().stream().limit(5).toList()));
        model.addAttribute("dueForReview", followUpService.open().size());
        return "console/dashboard";
    }

    @GetMapping("/followups")
    public String followUps(Authentication auth, Model model,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        who(auth, model, "Patients Due for Review", "followups");
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start.plusDays(7) : to;
        List<FollowUp> cases = followUpService.weeklyList(start, end);
        model.addAttribute("from", start); model.addAttribute("to", end);
        model.addAttribute("dueCount", cases.size());
        model.addAttribute("invalidContacts", cases.stream().filter(f -> !f.isContactReachable()).count());
        model.addAttribute("rows", cases);
        return "console/followups";
    }

    @PostMapping("/followups/{id}/queue")
    public String queueOne(@PathVariable Long id, Authentication auth) {
        followUpService.notify(id, auth.getName());
        return "redirect:/notifications";
    }

    @PostMapping("/followups/queue")
    public String queueCohort(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                              Authentication auth) {
        followUpService.queueCohort(from, to, auth.getName());
        return "redirect:/notifications";
    }

    @GetMapping("/notifications")
    public String notifications(Authentication auth, Model model) {
        who(auth, model, "Reminder Queue", "notifications");
        List<Notification> rows = notifications.findAllByOrderByIdDesc();
        model.addAttribute("queued", rows.stream().filter(Notification::isQueued).count());
        model.addAttribute("sent", rows.stream().filter(n -> n.getStatus() == NotificationStatus.SENT || n.getStatus() == NotificationStatus.DELIVERED).count());
        model.addAttribute("failed", rows.stream().filter(n -> n.getStatus() == NotificationStatus.FAILED).count());
        model.addAttribute("optedOut", rows.stream().filter(n -> n.getStatus() == NotificationStatus.EXCLUDED).count());
        model.addAttribute("retryPending", rows.stream().filter(n -> n.getStatus() == NotificationStatus.RETRY_PENDING).count());
        model.addAttribute("rows", rows);
        return "console/notifications";
    }

    @PostMapping("/notifications/{id}/sent")
    public String sent(@PathVariable Long id, @RequestParam String receipt, Authentication auth) {
        followUpService.markSent(id, receipt, auth.getName());
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/{id}/delivered")
    public String delivered(@PathVariable Long id, Authentication auth) {
        followUpService.markDelivered(id, auth.getName());
        return "redirect:/notifications";
    }

    @GetMapping("/reports/sales")
    public String sales(Authentication auth, Model model,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        who(auth, model, "Sales Report", "sales");
        LocalDate start = from == null ? YearMonth.now().atDay(1) : from;
        LocalDate end = to == null ? YearMonth.from(start).atEndOfMonth() : to;
        ReportingService.SalesSummary s = reporting.sales(start, end);
        model.addAttribute("from", start); model.addAttribute("to", end);
        model.addAttribute("monthTotal", money(s.billed()));
        model.addAttribute("monthBills", s.billCount()); model.addAttribute("monthAdvances", money(s.advances()));
        model.addAttribute("monthOutstanding", money(s.outstanding()));
        model.addAttribute("orphanPayments", s.orphanPayments()); model.addAttribute("trend", salesTrend());
        model.addAttribute("byCategory", categoryRows(s));
        return "console/report-sales";
    }

    @GetMapping(value = "/reports/sales.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<String> salesCsv(Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ReportingService.SalesSummary s = reporting.sales(from, to);
        Long userId = users.findByUsername(auth.getName()).orElseThrow().getId();
        reporting.saveSalesSnapshot(s.from(), s.to(), userId, auth.getName());
        String csv = "period_start,period_end,bills,total_billed,total_collected,advances,outstanding,orphan_payments\n"
                + s.from() + "," + s.to() + "," + s.billCount() + "," + s.billed() + ","
                + s.collected() + "," + s.advances() + "," + s.outstanding() + "," + s.orphanPayments() + "\n";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=nethcare-sales-" + from + "-to-" + to + ".csv")
                .contentType(MediaType.parseMediaType("text/csv")).body(csv);
    }

    @GetMapping("/reports/orders")
    public String orders(Authentication auth, Model model) {
        who(auth, model, "Order Status", "orders");
        ReportingService.OrderSummary s = reporting.orders();
        model.addAttribute("orderSplit", orderRows(s)); model.addAttribute("totalOrders", s.open());
        model.addAttribute("avgDays", 0); model.addAttribute("overdue", s.overdue()); model.addAttribute("urgent", s.urgent());
        return "console/report-orders";
    }

    @GetMapping("/reports/stock")
    public String stock(Authentication auth, Model model) {
        who(auth, model, "Stock Summary", "stock");
        ReportingService.StockSummary s = reporting.stock();
        model.addAttribute("rows", stockRows(s.lowStock())); model.addAttribute("lowCount", s.lowCount());
        model.addAttribute("totalItems", s.totalItems()); model.addAttribute("stockValue", money(s.stockValue()));
        return "console/report-stock";
    }

    @GetMapping("/audit")
    public String audit(Authentication auth, Model model,
                        @RequestParam(required = false) String user,
                        @RequestParam(required = false) String entity,
                        @RequestParam(required = false) AuditAction action,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        who(auth, model, "Audit Trail", "audit");
        List<AuditLog> logs = auditLogs.search(blankNull(user), blankNull(entity), action,
                from == null ? null : from.atStartOfDay(), to == null ? null : to.plusDays(1).atStartOfDay());
        model.addAttribute("rows", logs); model.addAttribute("chainValid", auditService.verifyChain());
        return "console/audit";
    }

    private void who(Authentication auth, Model model, String title, String active) {
        model.addAttribute("title", title); model.addAttribute("active", active);
        model.addAttribute("user", auth.getName()); model.addAttribute("role", roleOf(auth));
        model.addAttribute("mock", false);
    }
    private String roleOf(Authentication auth) { return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).findFirst().orElse("STAFF_NURSE"); }
    private String money(BigDecimal amount) { return String.format("%,.2f", amount); }
    private String blankNull(String value) { return value == null || value.isBlank() ? null : value; }
    private List<String[]> orderRows(ReportingService.OrderSummary s) {
        return List.of(new String[]{"Collected", s.byStatus().get(OrderStatus.COLLECTED).toString(), "s1"},
                new String[]{"At lab", s.byStatus().get(OrderStatus.LAB).toString(), "s2"},
                new String[]{"Ready", s.byStatus().get(OrderStatus.READY).toString(), "s3"},
                new String[]{"Placed", s.byStatus().get(OrderStatus.PLACED).toString(), "s4"});
    }
    private List<String[]> stockRows(List<StockItem> items) { return items.stream().map(i -> new String[]{
            i.getItemCode(), i.getName(), i.getCategory().label(), String.valueOf(i.available()), String.valueOf(i.getReorderLevel())}).toList(); }
    private List<String[]> categoryRows(ReportingService.SalesSummary summary) {
        return summary.byCategory().entrySet().stream().map(entry -> {
            BigDecimal share = summary.billed().signum() == 0 ? BigDecimal.ZERO
                    : entry.getValue().multiply(BigDecimal.valueOf(100))
                    .divide(summary.billed(), 1, java.math.RoundingMode.HALF_UP);
            return new String[]{entry.getKey(), money(entry.getValue()), share + "%"};
        }).toList();
    }
    private List<String[]> salesTrend() {
        List<YearMonth> months = java.util.stream.IntStream.rangeClosed(0, 4)
                .mapToObj(i -> YearMonth.now().minusMonths(4L - i)).toList();
        List<BigDecimal> totals = months.stream()
                .map(month -> reporting.sales(month.atDay(1), month.atEndOfMonth()).billed()).toList();
        BigDecimal top = totals.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return java.util.stream.IntStream.range(0, months.size()).mapToObj(i -> {
            int height = top.signum() == 0 ? 0 : totals.get(i).multiply(BigDecimal.valueOf(150))
                    .divide(top, 0, java.math.RoundingMode.HALF_UP).intValue();
            return new String[]{months.get(i).getMonth().toString().substring(0, 3), money(totals.get(i)), height + "px"};
        }).toList();
    }
}
