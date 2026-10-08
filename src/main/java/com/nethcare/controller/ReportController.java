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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final NotificationDispatchService dispatchService;
    private final PatientRepository patients;
    private final ExaminationRepository examinations;

    public ReportController(FollowUpService followUpService, FollowUpRepository followUps,
                            NotificationRepository notifications, AuditLogRepository auditLogs,
                            AuditService auditService, ReportingService reporting, UserRepository users,
                            NotificationDispatchService dispatchService,
                            PatientRepository patients, ExaminationRepository examinations) {
        this.followUpService = followUpService; this.followUps = followUps;
        this.notifications = notifications; this.auditLogs = auditLogs;
        this.auditService = auditService; this.reporting = reporting; this.users = users;
        this.dispatchService = dispatchService;
        this.patients = patients; this.examinations = examinations;
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
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(required = false) String preset,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false, defaultValue = "false") boolean highRisk) {
        who(auth, model, "Patients Due for Review", "followups");
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        String activePreset = preset != null ? preset : "";

        if ("today".equalsIgnoreCase(preset)) {
            start = today;
            end = today;
        } else if ("this_week".equalsIgnoreCase(preset)) {
            start = today;
            end = today.plusDays(7);
        } else if ("next_14".equalsIgnoreCase(preset)) {
            start = today;
            end = today.plusDays(14);
        } else if ("this_month".equalsIgnoreCase(preset)) {
            start = today;
            end = today.plusDays(30);
        } else if ("overdue".equalsIgnoreCase(preset)) {
            start = today.minusMonths(6);
            end = today.minusDays(1);
        } else if (from != null && to != null) {
            start = from;
            end = to;
            if (from.equals(today) && to.equals(today)) activePreset = "today";
            else if (from.equals(today) && to.equals(today.plusDays(7))) activePreset = "this_week";
            else if (from.equals(today) && to.equals(today.plusDays(14))) activePreset = "next_14";
            else if (from.equals(today) && to.equals(today.plusDays(30))) activePreset = "this_month";
            else if (to.isBefore(today)) activePreset = "overdue";
            else activePreset = "custom";
        } else if (from != null) {
            start = from;
            end = from.plusDays(7);
            activePreset = "custom";
        } else {
            start = today;
            end = today.plusDays(7);
            activePreset = "this_week";
        }

        FollowUpCategory parsedCat = null;
        if (category != null && !category.isBlank()) {
            try {
                parsedCat = FollowUpCategory.valueOf(category.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        final FollowUpCategory filterCat = parsedCat;

        List<FollowUp> cases = followUpService.weeklyList(start, end);
        if (highRisk) {
            cases = cases.stream().filter(FollowUp::isHighRisk).toList();
        }
        if (filterCat != null) {
            cases = cases.stream().filter(f -> f.getCategory() == filterCat).toList();
        }

        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("preset", activePreset);
        model.addAttribute("highRisk", highRisk);
        model.addAttribute("selectedCategory", filterCat);
        model.addAttribute("dueCount", cases.size());
        model.addAttribute("invalidContacts", cases.stream().filter(f -> !f.isContactReachable()).count());
        model.addAttribute("rows", cases);
        model.addAttribute("patients", patients.findAll().stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList());
        model.addAttribute("opticians", users.findByRoleOrderByFullNameAsc(Role.OPTICIAN));
        return "console/followups";
    }

    @PostMapping({"/followups", "/followups/schedule"})
    public String scheduleFollowUp(@RequestParam Long patientId,
                                   @RequestParam(required = false) Long assignedOpticianId,
                                   @RequestParam(required = false) Long originatingVisitId,
                                   @RequestParam FollowUpCategory category,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetReviewDate,
                                   @RequestParam(required = false, defaultValue = "false") boolean highRisk,
                                   @RequestParam(required = false) String clinicalNotes,
                                   Authentication auth,
                                   RedirectAttributes ra) {
        try {
            Long visitId = originatingVisitId;
            if (visitId == null) {
                visitId = examinations.findByPatientIdOrderByExamDateDesc(patientId)
                        .stream().findFirst().map(Examination::getId)
                        .orElse(null);
            }
            if (visitId == null) {
                ra.addFlashAttribute("caseError", "Patient has no recorded examinations yet. Please record an examination first.");
                return "redirect:/followups";
            }

            Long opticianId = assignedOpticianId;
            if (opticianId == null) {
                User currentUser = users.findByUsername(auth.getName()).orElse(null);
                if (currentUser != null && (currentUser.getRole() == Role.OPTICIAN || currentUser.getRole() == Role.ADMIN)) {
                    opticianId = currentUser.getId();
                } else {
                    opticianId = users.findByRoleOrderByFullNameAsc(Role.OPTICIAN).stream().findFirst().map(User::getId)
                            .orElse(null);
                }
            }

            if (opticianId == null) {
                ra.addFlashAttribute("caseError", "No optician available to assign this case.");
                return "redirect:/followups";
            }

            FollowUp saved = followUpService.createCase(patientId, opticianId, visitId, category,
                    targetReviewDate, highRisk, clinicalNotes, auth.getName());
            ra.addFlashAttribute("caseMessage", "Follow-up case scheduled for " + saved.getDueOn() + " (" + saved.getPatientName() + ")");
        } catch (Exception ex) {
            ra.addFlashAttribute("caseError", "Failed to schedule follow-up: " + ex.getMessage());
        }
        return "redirect:/followups";
    }

    @PostMapping("/followups/{id}/booked")
    public String markBooked(@PathVariable Long id,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookedOn,
                             @RequestParam(required = false) String note,
                             Authentication auth,
                             RedirectAttributes ra) {
        try {
            LocalDate appointmentDate = bookedOn != null ? bookedOn : LocalDate.now().plusDays(7);
            String noteText = note != null && !note.isBlank() ? note : "Patient agreed to appointment";
            followUpService.recordResponse(id, FollowUpOutcome.BOOKED, noteText, appointmentDate, auth.getName());
            ra.addFlashAttribute("caseMessage", "Case marked as BOOKED for " + appointmentDate);
        } catch (Exception ex) {
            ra.addFlashAttribute("caseError", "Error marking as booked: " + ex.getMessage());
        }
        return "redirect:/followups";
    }

    @PostMapping("/followups/{id}/declined")
    public String markDeclined(@PathVariable Long id,
                               @RequestParam(required = false) String note,
                               Authentication auth,
                               RedirectAttributes ra) {
        try {
            String noteText = note != null && !note.isBlank() ? note : "Patient declined follow-up appointment";
            followUpService.recordResponse(id, FollowUpOutcome.DECLINED, noteText, null, auth.getName());
            ra.addFlashAttribute("caseMessage", "Case marked as DECLINED");
        } catch (Exception ex) {
            ra.addFlashAttribute("caseError", "Error marking as declined: " + ex.getMessage());
        }
        return "redirect:/followups";
    }

    @PostMapping("/followups/{id}/attended")
    public String markAttended(@PathVariable Long id,
                               Authentication auth,
                               RedirectAttributes ra) {
        try {
            followUpService.markAttended(id, auth.getName());
            ra.addFlashAttribute("caseMessage", "Case marked as ATTENDED");
        } catch (Exception ex) {
            ra.addFlashAttribute("caseError", "Error marking as attended: " + ex.getMessage());
        }
        return "redirect:/followups";
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

        SmsGateway gateway = dispatchService.getGateway();
        model.addAttribute("smsEnabled", true);
        model.addAttribute("smsProvider", gateway.getProviderName());
        model.addAttribute("isSimulator", gateway.isSimulator());

        EmailGateway emailGateway = dispatchService.getEmailGateway();
        model.addAttribute("emailEnabled", true);
        model.addAttribute("emailProvider", emailGateway != null ? emailGateway.getProviderName() : "N/A");
        model.addAttribute("isEmailSimulator", emailGateway != null && emailGateway.isSimulator());
        return "console/notifications";
    }

    @PostMapping("/notifications/dispatch-now")
    public String dispatchNow(Authentication auth, RedirectAttributes ra) {
        NotificationDispatchService.DispatchResult res = dispatchService.dispatchDue(auth.getName());
        ra.addFlashAttribute("dispatchMessage",
                String.format("Dispatched due notifications: %d sent, %d failed. (Newly queued: %d)",
                        res.sentCount(), res.failedCount(), res.queuedCount()));
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/{id}/send-now")
    public String sendNow(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        boolean sent = dispatchService.dispatchById(id, auth.getName());
        if (sent) {
            ra.addFlashAttribute("dispatchMessage", "Notification #" + id + " sent successfully.");
        } else {
            ra.addFlashAttribute("dispatchError", "Failed to send notification #" + id + ". Check error / exceptions log.");
        }
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/send-direct")
    public String sendDirect(@RequestParam(required = false, defaultValue = "SMS") String channel,
                             @RequestParam(required = false) String phone,
                             @RequestParam(required = false) String destination,
                             @RequestParam(required = false) String recipientName,
                             @RequestParam String message,
                             Authentication auth,
                             RedirectAttributes ra) {
        try {
            String target = destination != null && !destination.isBlank() ? destination.trim() : (phone != null ? phone.trim() : "");
            boolean isEmail = "EMAIL".equalsIgnoreCase(channel) || target.contains("@");
            Notification n;
            if (isEmail) {
                n = followUpService.sendDirectEmail(target, recipientName, message, auth.getName());
            } else {
                n = followUpService.sendDirectSms(target, recipientName, message, auth.getName());
            }

            if (n.getStatus() == NotificationStatus.FAILED) {
                ra.addFlashAttribute("dispatchError", "Failed to queue message: " + n.getFailureReason());
                return "redirect:/notifications";
            }
            boolean sent = dispatchService.dispatchSingle(n, auth.getName());
            String channelLabel = isEmail ? "Email" : "SMS";
            if (sent) {
                ra.addFlashAttribute("dispatchMessage", channelLabel + " sent successfully to " + n.getDestination() + " (Receipt: " + n.getGatewayReceiptId() + ")");
            } else {
                ra.addFlashAttribute("dispatchMessage", channelLabel + " queued for next dispatch (Reference: " + n.getReference() + ")");
            }
        } catch (Exception ex) {
            ra.addFlashAttribute("dispatchError", "Error sending message: " + ex.getMessage());
        }
        return "redirect:/notifications";
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
        who(auth, model, "Order Status", "report-orders");
        ReportingService.OrderSummary s = reporting.orders();
        model.addAttribute("orderSplit", orderRows(s)); model.addAttribute("totalOrders", s.open());
        model.addAttribute("avgDays", 0); model.addAttribute("overdue", s.overdue()); model.addAttribute("urgent", s.urgent());
        return "console/report-orders";
    }

    @GetMapping("/reports/stock")
    public String stock(Authentication auth, Model model) {
        who(auth, model, "Stock Summary", "report-stock");
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
        model.addAttribute("rows", logs);
        model.addAttribute("chainValid", auditService.verifyChain());
        model.addAttribute("selectedUser", user);
        model.addAttribute("selectedEntity", entity);
        model.addAttribute("selectedAction", action);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
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
