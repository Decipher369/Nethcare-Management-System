package com.nethcare.controller;

import com.nethcare.exception.BusinessException;
import com.nethcare.model.Bill;
import com.nethcare.model.Order;
import com.nethcare.model.Payment;
import com.nethcare.model.PaymentMethod;
import com.nethcare.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bills and the money taken against them.
 *
 * No card details are stored anywhere. The method is a label for how the
 * customer handed the money over at the counter, and a card payment is recorded
 * as "paid by card" with no card number kept — the till did that, not us.
 */
@Controller
public class BillController {

    /** The share of a bill that has to be in before an order can go to the lab. */
    private static final BigDecimal ADVANCE_PERCENT = new BigDecimal("40");

    private final OrderService orders;

    public BillController(OrderService orders) {
        this.orders = orders;
    }

    /** Outstanding bills, which is what the counter actually works through. */
    @GetMapping("/bills")
    public String list(Authentication auth, Model model,
                       @RequestParam(name = "view", defaultValue = "outstanding") String view) {

        List<Bill> bills = orders.all().stream()
                .map(o -> {
                    try {
                        return orders.billFor(o.getId());
                    } catch (RuntimeException ex) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .filter(b -> {
                    if ("all".equals(view)) {
                        return true;
                    }
                    if ("settled".equals(view)) {
                        return !b.isCancelled() && b.isSettled();
                    }
                    if ("cancelled".equals(view)) {
                        return b.isCancelled();
                    }
                    return !b.isCancelled() && !b.isSettled();
                })
                .toList();

        model.addAttribute("bills", bills);
        model.addAttribute("view", view);
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("user", auth == null ? "staff" : auth.getName());
        model.addAttribute("active", "bills");
        return "bills/list";
    }

    @GetMapping("/bills/{id}")
    public String detail(Authentication auth, @PathVariable Long id, Model model) {
        Bill bill = orders.getBill(id);
        Order order = orders.get(bill.getOrderId());
        List<Payment> payments = orders.paymentsFor(id);

        model.addAttribute("bill", bill);
        model.addAttribute("order", order);
        model.addAttribute("payments", payments);
        model.addAttribute("methods", PaymentMethod.values());
        // The counter can see what is still needed for the lab even before they
        // try to advance the order, so the shortfall is not a surprise.
        model.addAttribute("advanceRequired", requiredAdvance(bill));
        model.addAttribute("user", auth == null ? "staff" : auth.getName());
        model.addAttribute("active", "bills");
        return "bills/detail";
    }

    @PostMapping("/bills/{id}/pay")
    public String pay(@PathVariable Long id,
                      @RequestParam BigDecimal amount,
                      @RequestParam(required = false) PaymentMethod method,
                      @RequestParam(name = "advance", defaultValue = "false") boolean advance,
                      Authentication auth, Model model) {
        Bill bill = orders.getBill(id);
        try {
            Payment p = orders.takePayment(id, amount, method, advance,
                    auth == null ? "staff" : auth.getName());
            model.addAttribute("msg", "Receipt " + p.getReceiptNo() + " recorded.");
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "redirect:/bills/" + id;
    }

    private BigDecimal requiredAdvance(Bill bill) {
        if (bill.isCancelled()) {
            return BigDecimal.ZERO;
        }
        return bill.getTotal()
                .multiply(ADVANCE_PERCENT)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
}
