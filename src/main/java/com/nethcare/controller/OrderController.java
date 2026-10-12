package com.nethcare.controller;

import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.Bill;
import com.nethcare.model.Order;
import com.nethcare.model.OrderItem;
import com.nethcare.model.OrderPriority;
import com.nethcare.model.OrderStatus;
import com.nethcare.service.OrderService;
import com.nethcare.service.StockService;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.PrescriptionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The order screens — the counter's worklist, one order, and the new-order form.
 *
 * OrderService already enforces the rules that matter (the 40% advance before
 * the lab, reservation on create, stock deducted on collection, credit note on
 * cancel). Nothing here re-decides any of it; the screens show what the service
 * decided and let the service refuse the rest.
 */
@Controller
public class OrderController {

    private final OrderService orders;
    private final StockService stock;
    private final PatientRepository patients;
    private final PrescriptionRepository prescriptions;

    public OrderController(OrderService orders, StockService stock,
                           PatientRepository patients, PrescriptionRepository prescriptions) {
        this.orders = orders;
        this.stock = stock;
        this.patients = patients;
        this.prescriptions = prescriptions;
    }

    /**
     * The worklist. Defaults to the open orders, because a counter asking
     * "where is my order" wants the ones still moving, not the archive.
     */
    @GetMapping("/orders")
    public String list(Authentication auth, Model model,
                       @RequestParam(name = "view", defaultValue = "open") String view,
                       @RequestParam(name = "q", required = false) String q) {

        List<Order> found;
        if ("all".equals(view)) {
            found = orders.all();
        } else if ("overdue".equals(view)) {
            found = orders.overdue();
        } else if (view != null && !view.isBlank() && !"open".equals(view)) {
            try {
                found = orders.byStatus(OrderStatus.valueOf(view.toUpperCase()));
            } catch (IllegalArgumentException e) {
                found = orders.open();
            }
        } else {
            found = orders.open();
        }

        if (q != null && !q.isBlank()) {
            String term = q.trim().toLowerCase();
            found = found.stream()
                    .filter(o -> contains(o.getOrderNo(), term) || contains(o.getCustomerName(), term))
                    .toList();
        }

        model.addAttribute("orders", found);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("counts", countSummary());
        model.addAttribute("view", view);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("user", auth == null ? "staff" : auth.getName());
        model.addAttribute("active", "orders");
        return "orders/list";
    }

    @GetMapping("/orders/new")
    public String newOrderForm(Authentication auth, Model model,
                               @RequestParam(name = "patientId", required = false) Long patientId) {
        Order order = new Order();
        order.setPatientId(patientId);
        model.addAttribute("order", order);
        populateOrderForm(model, patientId, auth);
        return "orders/form";
    }

    @PostMapping("/orders/new")
    public String place(@ModelAttribute("order") Order order,
                        @RequestParam(name = "stockItemId", required = false) List<Long> stockItemIds,
                        @RequestParam(name = "quantity", required = false) List<Integer> quantities,
                        Authentication auth,
                        Model model) {
        try {
            List<OrderItem> lines = buildLines(stockItemIds, quantities);
            Order saved = orders.place(order, lines, auth == null ? "staff" : auth.getName());
            return "redirect:/orders/" + saved.getId();
        } catch (BusinessException ex) {
            // The service already refused. Send the form back as it was typed so
            // the counter does not have to retype the order.
            model.addAttribute("order", order);
            populateOrderForm(model, order.getPatientId(), auth);
            model.addAttribute("error", ex.getMessage());
            return "orders/form";
        }
    }

    @GetMapping("/orders/{id}")
    public String detail(Authentication auth, @PathVariable Long id, Model model) {
        Order order = orders.get(id);
        Bill bill = billOrNull(id);
        model.addAttribute("order", order);
        model.addAttribute("bill", bill);
        model.addAttribute("next", order.getStatus().next());
        model.addAttribute("blockedBy", blockReason(order, bill));
        model.addAttribute("canCancel", order.getStatus().canBeCancelled());
        model.addAttribute("user", auth == null ? "staff" : auth.getName());
        model.addAttribute("active", "orders");
        return "orders/detail";
    }

    /**
     * Why the next step would be refused, or null when it would go through.
     * The service keeps the rules; this just restates the one the counter is
     * most likely to hit, so the page explains itself before the click.
     */
    private String blockReason(Order order, Bill bill) {
        if (bill == null) {
            return "Raise the bill before this order can move on.";
        }
        if (bill.isCancelled()) {
            return "This bill is cancelled, so the order cannot move on.";
        }
        if (order.getStatus() == OrderStatus.READY
                && bill.balance().compareTo(BigDecimal.ZERO) > 0) {
            return "LKR " + bill.balance().toPlainString()
                    + " is still owing. Settle the bill before handing this over.";
        }
        return null;
    }

    /** One step along the pipeline. The 40% rule is checked by the service. */
    @PostMapping("/orders/{id}/advance")
    public String advance(Authentication auth, @PathVariable Long id, Model model) {
        Order order = orders.get(id);
        try {
            Order saved = orders.advance(id, auth == null ? "staff" : auth.getName());
            model.addAttribute("msg", order.getOrderNo() + " is now " + saved.getStatus().label().toLowerCase() + ".");
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(name = "reason", required = false) String reason,
                         Authentication auth,
                         Model model) {
        Order order = orders.get(id);
        try {
            orders.cancel(id, reason, auth == null ? "staff" : auth.getName());
            model.addAttribute("msg", "Order " + order.getOrderNo() + " cancelled. Reserved stock was released.");
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    /**
     * Raises the bill for this order. A discount is optional; the urgent
     * surcharge is the service's call, not the form's.
     */
    @PostMapping("/orders/{id}/bill")
    public String raiseBill(@PathVariable Long id,
                            @RequestParam(name = "discount", required = false) BigDecimal discount,
                            Authentication auth, Model model) {
        Order order = orders.get(id);
        try {
            Bill bill = orders.generateBill(id, discount, null);
            model.addAttribute("msg", "Bill " + bill.getBillNo() + " raised for " + order.getOrderNo() + ".");
            return "redirect:/bills/" + bill.getId();
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "redirect:/orders/" + id;
        }
    }

    // ---------------------------------------------------------------- helpers

    private Bill billOrNull(Long orderId) {
        try {
            return orders.billFor(orderId);
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }

    private void populateOrderForm(Model model, Long patientId, Authentication auth) {
        model.addAttribute("patients", patients.findAllByOrderByFullNameAsc());
        model.addAttribute("selectedPatient", patientId == null
                ? null : patients.findById(patientId).orElse(null));
        model.addAttribute("prescriptions", patientId == null
                ? List.of() : prescriptions.findByPatientIdOrderByIssuedOnDesc(patientId));
        model.addAttribute("stock", stock.listed());
        model.addAttribute("priorities", OrderPriority.values());
        model.addAttribute("user", auth == null ? "staff" : auth.getName());
        model.addAttribute("active", "orders");
    }

    // The form posts one quantity box per catalogue row, so an untouched row
    // arrives as 0 rather than being absent. Anything at 1 or more is a line.
    private List<OrderItem> buildLines(List<Long> ids, List<Integer> amounts) {
        List<OrderItem> lines = new java.util.ArrayList<>();
        if (ids == null) {
            return lines;
        }
        for (int i = 0; i < ids.size(); i++) {
            int qty = (amounts == null || i >= amounts.size() || amounts.get(i) == null) ? 0 : amounts.get(i);
            if (qty <= 0) {
                continue;
            }
            OrderItem line = new OrderItem();
            line.setStockItemId(ids.get(i));
            line.setQuantity(qty);
            lines.add(line);
        }
        return lines;
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    // Small card of counts so the counter can see the day's shape without
    // filtering first.
    private java.util.Map<String, Long> countSummary() {
        java.util.Map<String, Long> m = new java.util.LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            m.put(s.name(), orders.countByStatus(s));
        }
        return m;
    }
}
