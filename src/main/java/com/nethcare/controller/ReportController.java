package com.nethcare.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Module 4 — Follow-up, Reporting & Audit.
 *
 * These screens are built on their own branch off main, so the tables they
 * read (patients from M1, examinations from M2, orders and bills from M3) are
 * not in the tree yet. Every figure below is therefore a placeholder drawn
 * from the client's own mock-up — LKR 842K, 196 patients, 141 orders, 7 items
 * below reorder — and each screen says so at the top rather than passing the
 * numbers off as real.
 *
 * What is not a placeholder is the layout, the columns, the filter controls
 * and the wording. Those are the screen design, and they carry over to the
 * live queries unchanged; only the numbers get replaced at the merge.
 *
 * Edwien — Follow-up, Reporting & Audit.
 */
@Controller
public class ReportController {

    /** Monthly sales, Apr–Aug, from the deck's dashboard sketch. */
    private static final Map<String, BigDecimal> SALES = Map.of(
            "Apr", new BigDecimal("610000"),
            "May", new BigDecimal("724000"),
            "Jun", new BigDecimal("688000"),
            "Jul", new BigDecimal("795000"),
            "Aug", new BigDecimal("842000"));

    private static final Map<String, Long> ORDER_STATUS = Map.of(
            "Collected", 84L,
            "At lab", 26L,
            "Ready", 22L,
            "Placed", 9L);

    private static final List<String[]> LOW_STOCK = List.of(
            new String[]{"TR-204", "Titan TR-204", "Frames", "2", "4"},
            new String[]{"SV156-CLR", "SV lens 1.56 clear", "SV lens", "1", "5"},
            new String[]{"BF-173", "Bifocal 1.67", "Bifocal", "3", "3"},
            new String[]{"CL-MOIST", "Monthly contacts", "Contacts", "0", "10"},
            new String[]{"CL-DAILY", "Daily contacts", "Contacts", "4", "6"},
            new String[]{"CS-009", "Hard case", "Cases", "2", "8"},
            new String[]{"LN-SPG", "Progressive 1.74", "SV lens", "1", "3"});

    // ------------------------------------------------------------ dashboard

    @GetMapping("/dashboard/console")
    public String dashboard(Authentication auth, Model model) {
        who(auth, model, "Dashboard");
        model.addAttribute("active", "dashboard");
        model.addAttribute("salesThisMonth", money(SALES.get("Aug")));
        model.addAttribute("patientsAttended", 196L);
        model.addAttribute("ordersCompleted", 141L);
        model.addAttribute("lowStockCount", LOW_STOCK.size());
        model.addAttribute("trend", barTrend());
        model.addAttribute("orderSplit", List.of(
                new String[]{"Collected", "84", "s1"},
                new String[]{"At lab", "26", "s2"},
                new String[]{"Ready", "22", "s3"},
                new String[]{"Placed", "9", "s4"}));
        model.addAttribute("totalOrders", 141L);
        model.addAttribute("lowStock", LOW_STOCK.subList(0, 5));
        model.addAttribute("dueForReview", 23L);
        return "console/dashboard";
    }

    // ------------------------------------------------------------- follow-up

    @GetMapping("/followups")
    public String followUps(Model model) {
        who(null, model, "Patients Due for Review");
        model.addAttribute("active", "followups");
        model.addAttribute("dueCount", 23L);
        model.addAttribute("invalidContacts", 4L);
        model.addAttribute("rows", List.of(
                new String[]{"P-0148", "K. N. Perera", "071 234 5678", "12 Aug 2025",
                             "12 Aug 2026", "6 months", "warn", "Call today"},
                new String[]{"P-0203", "S. Fernando", "077 998 2211", "03 Mar 2025",
                             "03 Mar 2026", "12 months", "bad", "Number invalid"},
                new String[]{"P-0111", "N. Wickramasinghe", "070 123 4455", "21 Jan 2026",
                             "21 Jan 2027", "6 months", "good", "Reminder sent"},
                new String[]{"P-0452", "A. Silva", "071 445 8890", "19 Apr 2025",
                             "19 Apr 2026", "12 months", "info", "No response"},
                new String[]{"P-0301", "R. Jayawardena", "076 332 1098", "28 Feb 2026",
                             "28 Feb 2027", "6 months", "good", "Booked 04 Oct"}));
        return "console/followups";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        who(null, model, "Reminder Queue");
        model.addAttribute("active", "notifications");
        model.addAttribute("queued", 12L);
        model.addAttribute("sent", 178L);
        model.addAttribute("failed", 4L);
        model.addAttribute("optedOut", 7L);
        model.addAttribute("rows", List.of(
                new String[]{"RCP-0311", "SMS", "K. N. Perera", "12 Aug 2026 08:00",
                             "good", "Sent", "Delivered"},
                new String[]{"RCP-0312", "Email", "S. Fernando", "12 Aug 2026 08:00",
                             "mute", "Skipped", "Opted out"},
                new String[]{"RCP-0313", "SMS", "N. Wickramasinghe", "12 Aug 2026 08:00",
                             "good", "Sent", "Delivered"},
                new String[]{"RCP-0314", "SMS", "A. Silva", "12 Aug 2026 08:00",
                             "bad", "Failed", "Invalid number"},
                new String[]{"RCP-0315", "Email", "R. Jayawardena", "12 Aug 2026 08:00",
                             "info", "Queued", "Runs 08:00 Monday"}));
        return "console/notifications";
    }

    // --------------------------------------------------------------- reports

    @GetMapping("/reports/sales")
    public String sales(Model model) {
        who(null, model, "Sales Report");
        model.addAttribute("active", "sales");
        model.addAttribute("trend", barTrend());
        model.addAttribute("monthTotal", "842,000");
        model.addAttribute("monthBills", 141L);
        model.addAttribute("monthAdvances", "398,000");
        model.addAttribute("monthOutstanding", "61,500");
        model.addAttribute("byCategory", List.of(
                new String[]{"Spectacles", "612,000", "72.7%"},
                new String[]{"Contact lenses", "141,000", "16.7%"},
                new String[]{"Clinical fees", "58,000", "6.9%"},
                new String[]{"Repairs & adjustments", "31,000", "3.7%"}));
        return "console/report-sales";
    }

    @GetMapping("/reports/orders")
    public String orders(Model model) {
        who(null, model, "Order Status");
        model.addAttribute("active", "orders");
        model.addAttribute("orderSplit", List.of(
                new String[]{"Collected", "84", "s1"},
                new String[]{"At lab", "26", "s2"},
                new String[]{"Ready", "22", "s3"},
                new String[]{"Placed", "9", "s4"}));
        model.addAttribute("totalOrders", 141L);
        model.addAttribute("avgDays", 9L);
        model.addAttribute("overdue", 3L);
        model.addAttribute("urgent", 12L);
        return "console/report-orders";
    }

    @GetMapping("/reports/stock")
    public String stock(Model model) {
        who(null, model, "Stock Summary");
        model.addAttribute("active", "stock");
        model.addAttribute("rows", LOW_STOCK);
        model.addAttribute("lowCount", LOW_STOCK.size());
        model.addAttribute("totalItems", 63L);
        model.addAttribute("stockValue", "1,284,000");
        return "console/report-stock";
    }

    // ----------------------------------------------------------------- audit

    @GetMapping("/audit")
    public String audit(Model model) {
        who(null, model, "Audit Trail");
        model.addAttribute("active", "audit");
        model.addAttribute("rows", List.of(
                new String[]{"2026-09-28 14:02", "optician", "Examination", "CREATE", "#2411", "—"},
                new String[]{"2026-09-28 13:47", "staff", "Order", "UPDATE", "ORD-0091", "PLACED → LAB"},
                new String[]{"2026-09-28 11:20", "admin", "StockItem", "UPDATE", "TR-204", "price 6,200 → 6,500"},
                new String[]{"2026-09-28 10:05", "staff", "Payment", "CREATE", "RCP-0311", "LKR 5,000"},
                new String[]{"2026-09-27 16:33", "surgeon", "Referral", "UPDATE", "RF-0088", "feedback added"},
                new String[]{"2026-09-27 15:12", "optician", "Patient", "UPDATE", "P-0148", "phone changed"},
                new String[]{"2026-09-27 09:44", "admin", "User", "CREATE", "surgeon", "role SURGEON"}));
        return "console/audit";
    }

    // ---------------------------------------------------------------- shared

    /**
     * Fills the sidebar and topbar. When there is no session — which is the
     * case while these screens are still wireframes — it shows a placeholder
     * rather than failing, so the layout can be reviewed before the security
     * rules are wired in.
     */
    private void who(Authentication auth, Model model, String title) {
        model.addAttribute("title", title);
        model.addAttribute("user", auth == null ? "staff" : auth.getName());
        model.addAttribute("role", auth == null ? "STAFF_NURSE" : roleOf(auth));
        model.addAttribute("mock", true);
    }

    private String roleOf(Authentication auth) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (a.getAuthority().startsWith("ROLE_")) {
                return a.getAuthority().substring(5);
            }
        }
        return "STAFF_NURSE";
    }

    /** 842000 -> "842,000". Money is easier to read grouped. */
    private static String money(BigDecimal amount) {
        return String.format("%,d", amount.longValue());
    }

    /**
     * Bar heights in pixels, tallest month = 150px. Doing the division here
     * keeps the arithmetic out of the template.
     */
    private List<String[]> barTrend() {
        BigDecimal top = SALES.values().stream()
                .max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        return SALES.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(e -> new String[]{
                        e.getKey(),
                        money(e.getValue()),
                        e.getValue().multiply(BigDecimal.valueOf(150))
                                .divide(top, 0, BigDecimal.ROUND_HALF_UP)
                                .intValue() + "px"})
                .toList();
    }

    /** Used by the templates for "days until" style hints. */
    static LocalDate today() {
        return LocalDate.now();
    }
}
