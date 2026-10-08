package com.nethcare.controller;

import com.nethcare.dto.PatientDto;
import com.nethcare.dto.PortalData;
import com.nethcare.model.*;
import com.nethcare.repository.*;
import com.nethcare.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Whole Website Health & Verification Test.
 * Validates HTTP status, template resolution, security authorization,
 * model bindings, and absence of 500 runtime errors across all application modules.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WholeWebsiteHealthTest {

    @Autowired MockMvc mvc;

    @MockBean PatientRepository patientRepo;
    @MockBean PatientService patientService;
    @MockBean ClinicalService clinicalService;
    @MockBean FollowUpService followUpService;
    @MockBean FollowUpRepository followUpRepo;
    @MockBean NotificationDispatchService dispatchService;
    @MockBean NotificationRepository notificationRepo;
    @MockBean OrderService orderService;
    @MockBean StockService stockService;
    @MockBean ReportingService reportingService;
    @MockBean AuditService auditService;
    @MockBean AuditLogRepository auditLogRepo;
    @MockBean UserService userService;
    @MockBean UserRepository userRepo;
    @MockBean LoginEventRepository loginEventRepo;
    @MockBean PortalService portalService;
    @MockBean ExaminationRepository examRepo;
    @MockBean PrescriptionRepository prescriptionRepo;
    @MockBean ReferralRepository referralRepo;

    private Patient samplePatient;
    private Order sampleOrder;
    private Bill sampleBill;
    private StockItem sampleStock;
    private Referral sampleReferral;
    private User adminUser;

    @BeforeEach
    void setUp() {
        samplePatient = new Patient();
        samplePatient.setId(10L);
        samplePatient.setPatientNo("P-0010");
        samplePatient.setFullName("Sunil Perera");
        samplePatient.setPhone("0712345678");
        samplePatient.setRegisteredOn(LocalDate.now());
        samplePatient.setIsActive(true);

        sampleOrder = new Order();
        sampleOrder.setId(100L);
        sampleOrder.setOrderNo("ORD-0100");
        sampleOrder.setPatientId(10L);
        sampleOrder.setPatientNoSnapshot("P-0010");
        sampleOrder.setCustomerName("Sunil Perera");
        sampleOrder.setStatus(OrderStatus.PLACED);
        sampleOrder.setOrderedOn(LocalDate.now());

        sampleBill = new Bill();
        sampleBill.setId(200L);
        sampleBill.setBillNo("BIL-0200");
        sampleBill.setOrderId(sampleOrder.getId());
        sampleBill.setPatientId(10L);
        sampleBill.setTotal(new BigDecimal("15000.00"));

        sampleStock = new StockItem();
        sampleStock.setId(300L);
        sampleStock.setItemCode("FRM-001");
        sampleStock.setName("Classic Aviator");
        sampleStock.setCategory(StockCategory.FRAME);
        sampleStock.setQuantity(25);
        sampleStock.setReorderLevel(5);
        sampleStock.setUnitPrice(new BigDecimal("8500.00"));

        sampleReferral = new Referral();
        sampleReferral.setId(400L);
        sampleReferral.setRefNo("REF-0400");
        sampleReferral.setPatientId(10L);
        sampleReferral.setSurgeonName("surgeon");
        sampleReferral.setStatus("PENDING");
        sampleReferral.setReferredOn(LocalDate.now());
        sampleReferral.setReferredBy("optician");
        sampleReferral.setReason("Cataract check");
        sampleReferral.setUrgency("ROUTINE");

        adminUser = new User("admin", "hash", Role.ADMIN);
        adminUser.setId(1L);
        adminUser.setFullName("Admin User");

        // Common mocks
        when(patientRepo.findByIsActiveTrueOrderByFullNameAsc()).thenReturn(List.of(samplePatient));
        when(patientRepo.findByIsActiveFalseOrderByFullNameAsc()).thenReturn(List.of());
        when(patientRepo.findAllByOrderByFullNameAsc()).thenReturn(List.of(samplePatient));
        when(patientRepo.search(anyString())).thenReturn(List.of(samplePatient));
        when(patientRepo.findById(10L)).thenReturn(Optional.of(samplePatient));
        when(patientService.get(10L)).thenReturn(samplePatient);

        when(orderService.open()).thenReturn(List.of(sampleOrder));
        when(orderService.all()).thenReturn(List.of(sampleOrder));
        when(orderService.overdue()).thenReturn(List.of());
        when(orderService.byStatus(any())).thenReturn(List.of(sampleOrder));
        when(orderService.get(100L)).thenReturn(sampleOrder);
        when(orderService.billFor(100L)).thenReturn(sampleBill);
        when(orderService.getBill(200L)).thenReturn(sampleBill);
        when(orderService.paymentsFor(200L)).thenReturn(List.of());

        when(stockService.listed()).thenReturn(List.of(sampleStock));
        when(stockService.lowStock()).thenReturn(List.of());
        when(stockService.search(any(), any())).thenReturn(List.of(sampleStock));
        when(stockService.gallery(any(), any())).thenReturn(List.of(sampleStock));
        when(stockService.get(300L)).thenReturn(sampleStock);

        when(referralRepo.findAllByOrderByReferredOnDesc()).thenReturn(List.of(sampleReferral));
        when(referralRepo.findByStatusOrderByReferredOnDesc(anyString())).thenReturn(List.of(sampleReferral));
        when(referralRepo.findBySurgeonNameOrderByReferredOnDesc(anyString())).thenReturn(List.of(sampleReferral));
        when(referralRepo.findBySurgeonNameAndStatusOrderByReferredOnDesc(anyString(), anyString())).thenReturn(List.of(sampleReferral));
        when(referralRepo.findById(400L)).thenReturn(Optional.of(sampleReferral));

        Map<OrderStatus, Long> statusMap = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) statusMap.put(s, 0L);
        statusMap.put(OrderStatus.COLLECTED, 5L);
        statusMap.put(OrderStatus.LAB, 2L);
        statusMap.put(OrderStatus.READY, 1L);
        statusMap.put(OrderStatus.PLACED, 3L);

        ReportingService.OrderSummary orderSummary = new ReportingService.OrderSummary(11L, 6L, 1L, 0L, statusMap);
        ReportingService.StockSummary stockSummary = new ReportingService.StockSummary(100L, 2L, new BigDecimal("85000.00"), List.of(sampleStock));
        Map<String, BigDecimal> catMap = new LinkedHashMap<>();
        catMap.put("FRAME", new BigDecimal("15000.00"));
        ReportingService.SalesSummary salesSummary = new ReportingService.SalesSummary(
                YearMonth.now().atDay(1), LocalDate.now(), 10L,
                new BigDecimal("150000.00"), new BigDecimal("120000.00"), new BigDecimal("30000.00"),
                new BigDecimal("30000.00"), catMap, 0L);

        when(reportingService.sales(any(), any())).thenReturn(salesSummary);
        when(reportingService.orders()).thenReturn(orderSummary);
        when(reportingService.stock()).thenReturn(stockSummary);

        when(auditService.verifyChain()).thenReturn(true);
        when(auditLogRepo.search(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(auditLogRepo.findAll()).thenReturn(List.of());

        when(notificationRepo.findAllByOrderByIdDesc()).thenReturn(List.of());
        when(followUpRepo.countByStatusIn(any())).thenReturn(0L);

        when(dispatchService.getGateway()).thenReturn(new SmsGateway() {
            @Override public String send(String destination, String message) { return "mock-rec"; }
        });
        when(dispatchService.getEmailGateway()).thenReturn(new EmailGateway() {
            @Override public String send(String destination, String subject, String message) { return "mock-rec"; }
        });

        when(userRepo.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(userRepo.findByRoleOrderByFullNameAsc(any())).thenReturn(List.of(adminUser));
        when(userService.list()).thenReturn(List.of(adminUser));
        when(loginEventRepo.findTop200ByOrderByOccurredAtDesc()).thenReturn(List.of());
    }

    // ==========================================
    // 1. PUBLIC PAGES (Unauthenticated)
    // ==========================================

    @Test
    @DisplayName("Public: Home landing page renders with 200 OK")
    void publicHomeRenders() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/home"))
                .andExpect(content().string(containsString("Nethcare")));
    }

    @Test
    @DisplayName("Public: About page renders with 200 OK")
    void publicAboutRenders() throws Exception {
        mvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/about"));
    }

    @Test
    @DisplayName("Public: Contact page renders with 200 OK")
    void publicContactRenders() throws Exception {
        mvc.perform(get("/contact"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/contact"));
    }

    @Test
    @DisplayName("Public: Frames catalog renders with 200 OK")
    void publicFramesRenders() throws Exception {
        mvc.perform(get("/frames"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/frames"));
    }

    @Test
    @DisplayName("Public: Login screen renders with 200 OK and CSRF token")
    void loginPageRenders() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    // ==========================================
    // 2. ADMIN ROLE PAGES (Full Workspace Crawl)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Dashboard redirects to /dashboard/console")
    void adminDashboardRedirects() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/console"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Console dashboard renders cleanly without 500 error")
    void adminConsoleDashboardRenders() throws Exception {
        mvc.perform(get("/dashboard/console"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/dashboard"))
                .andExpect(content().string(containsString("console")))
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Patients list, active/inactive/all filters and search render 200 OK")
    void adminPatientsListAndFiltersRender() throws Exception {
        mvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/list"))
                .andExpect(content().string(containsString("patients-topbar")));

        mvc.perform(get("/patients").param("status", "inactive"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/list"));

        mvc.perform(get("/patients").param("status", "all"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/list"));

        mvc.perform(get("/patients").param("q", "Sunil"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Patient registration form renders 200 OK")
    void adminPatientNewFormRenders() throws Exception {
        mvc.perform(get("/patients/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/form"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Patient detail page renders 200 OK")
    void adminPatientDetailRenders() throws Exception {
        when(clinicalService.historyFor(10L)).thenReturn(List.of());
        when(clinicalService.prescriptionsFor(10L)).thenReturn(List.of());

        mvc.perform(get("/patients/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/detail"))
                .andExpect(content().string(containsString("Sunil Perera")));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Referrals worklist renders 200 OK")
    void adminReferralsWorklistRenders() throws Exception {
        mvc.perform(get("/referrals"))
                .andExpect(status().isOk())
                .andExpect(view().name("clinical/referral-list"));

        mvc.perform(get("/referrals").param("status", "all"))
                .andExpect(status().isOk())
                .andExpect(view().name("clinical/referral-list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Follow-ups review console and timeframe presets render 200 OK")
    void adminFollowupsAndPresetsRender() throws Exception {
        String[] presets = {"today", "this_week", "next_14", "this_month", "overdue"};
        for (String preset : presets) {
            mvc.perform(get("/followups").param("preset", preset))
                    .andExpect(status().isOk())
                    .andExpect(view().name("console/followups"))
                    .andExpect(content().string(containsString("filter-pills-bar")));
        }

        mvc.perform(get("/followups").param("highRisk", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/followups"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Notifications queue console renders 200 OK")
    void adminNotificationsRenders() throws Exception {
        mvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/notifications"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Stock worklist, filters, and forms render 200 OK")
    void adminStockViewsRender() throws Exception {
        mvc.perform(get("/stock"))
                .andExpect(status().isOk())
                .andExpect(view().name("stock/list"));

        mvc.perform(get("/stock").param("view", "low"))
                .andExpect(status().isOk())
                .andExpect(view().name("stock/list"));

        mvc.perform(get("/stock").param("category", "").param("view", "all"))
                .andExpect(status().isOk())
                .andExpect(view().name("stock/list"));

        mvc.perform(get("/stock/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("stock/form"));

        mvc.perform(get("/stock/300/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("stock/form"));

        mvc.perform(get("/stock/300/count"))
                .andExpect(status().isOk())
                .andExpect(view().name("stock/count"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Orders list, new order form, and detail render 200 OK")
    void adminOrdersViewsRender() throws Exception {
        mvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"));

        mvc.perform(get("/orders").param("view", "all"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"));

        mvc.perform(get("/orders").param("view", "overdue"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"));

        mvc.perform(get("/orders").param("view", "nonexistent_status"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"));

        mvc.perform(get("/orders/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/form"));

        mvc.perform(get("/orders/100"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/detail"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Bills list, settled/cancelled filters, and detail render 200 OK")
    void adminBillsViewsRender() throws Exception {
        mvc.perform(get("/bills"))
                .andExpect(status().isOk())
                .andExpect(view().name("bills/list"));

        mvc.perform(get("/bills").param("view", "settled"))
                .andExpect(status().isOk())
                .andExpect(view().name("bills/list"));

        mvc.perform(get("/bills").param("view", "cancelled"))
                .andExpect(status().isOk())
                .andExpect(view().name("bills/list"));

        mvc.perform(get("/bills/200"))
                .andExpect(status().isOk())
                .andExpect(view().name("bills/detail"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Reports (Sales, CSV, Orders, Stock) and Audit render 200 OK")
    void adminReportsAndAuditRender() throws Exception {
        mvc.perform(get("/reports/sales"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/report-sales"));

        mvc.perform(get("/reports/sales").param("period", "this_month"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/report-sales"));

        mvc.perform(get("/reports/sales.csv").param("from", "2026-01-01").param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));

        mvc.perform(get("/reports/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/report-orders"));

        mvc.perform(get("/reports/stock"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/report-stock"));

        mvc.perform(get("/audit"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/audit"));

        mvc.perform(get("/audit").param("entity", "Patient"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/audit"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: User administration renders 200 OK")
    void adminUsersPageRenders() throws Exception {
        mvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(containsString("user-admin-page")));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    @DisplayName("Admin: Account password change page renders 200 OK")
    void adminChangePasswordPageRenders() throws Exception {
        mvc.perform(get("/account/change-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/change-password"));
    }

    // ==========================================
    // 3. ROLE-BASED ACCESS & SECURITY BOUNDARIES
    // ==========================================

    @Test
    @WithMockUser(roles = "OPTICIAN", username = "optician")
    @DisplayName("Optician: Lands on /patients, views console dashboard with vertical nav, cannot access /admin/users (403 Forbidden)")
    void opticianRoleAccess() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients"));

        mvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/notifications\"")));
        mvc.perform(get("/followups")).andExpect(status().isOk());
        mvc.perform(get("/dashboard/console"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/dashboard"))
                .andExpect(content().string(containsString("Patient care")))
                .andExpect(content().string(containsString("Patients")))
                .andExpect(content().string(containsString("Referrals")))
                .andExpect(content().string(containsString("Follow-ups")))
                .andExpect(content().string(containsString("Notifications")))
                .andExpect(content().string(not(containsString("Store operations"))));
        mvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF_NURSE", username = "staff")
    @DisplayName("Staff Nurse: Lands on /dashboard/console, accesses stock/orders with scoped nav, cannot access /audit")
    void staffNurseRoleAccess() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/console"));

        mvc.perform(get("/dashboard/console"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/dashboard"))
                .andExpect(content().string(containsString("Store operations")))
                .andExpect(content().string(containsString("Stock")))
                .andExpect(content().string(containsString("Orders")))
                .andExpect(content().string(containsString("Bills")))
                .andExpect(content().string(not(containsString("Patient care"))));
        mvc.perform(get("/stock")).andExpect(status().isOk());
        mvc.perform(get("/orders")).andExpect(status().isOk());
        mvc.perform(get("/bills")).andExpect(status().isOk());
        mvc.perform(get("/audit")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AUDITOR", username = "auditor")
    @DisplayName("Auditor: Lands on /reports/sales, accesses audit, views console with report nav, cannot access /patients")
    void auditorRoleAccess() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/sales"));

        mvc.perform(get("/dashboard/console"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/dashboard"))
                .andExpect(content().string(containsString("Reports")))
                .andExpect(content().string(containsString("Sales report")))
                .andExpect(content().string(containsString("Accountability")))
                .andExpect(content().string(containsString("Audit trail")))
                .andExpect(content().string(not(containsString("Store operations"))))
                .andExpect(content().string(not(containsString("Patient care"))));
        mvc.perform(get("/reports/sales")).andExpect(status().isOk());
        mvc.perform(get("/audit")).andExpect(status().isOk());
        mvc.perform(get("/patients")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SURGEON", username = "surgeon")
    @DisplayName("Surgeon: Lands on /referrals, accesses referrals, cannot access /stock")
    void surgeonRoleAccess() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/referrals"));

        mvc.perform(get("/referrals")).andExpect(status().isOk());
        mvc.perform(get("/stock")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PATIENT", username = "patient")
    @DisplayName("Patient: Lands on /portal, views personal history, cannot access staff console")
    void patientRoleAccess() throws Exception {
        PortalData portalData = new PortalData(PatientDto.of(samplePatient), List.of(), List.of());
        when(portalService.forUser("patient")).thenReturn(portalData);

        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal"));

        mvc.perform(get("/portal"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/home"));

        mvc.perform(get("/patients")).andExpect(status().isForbidden());
        mvc.perform(get("/orders")).andExpect(status().isForbidden());
    }
}
