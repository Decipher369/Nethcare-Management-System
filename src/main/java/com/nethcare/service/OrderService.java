package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.Bill;
import com.nethcare.model.Order;
import com.nethcare.model.OrderItem;
import com.nethcare.model.OrderPriority;
import com.nethcare.model.OrderStatus;
import com.nethcare.model.Payment;
import com.nethcare.model.PaymentMethod;
import com.nethcare.model.StockItem;
import com.nethcare.repository.BillRepository;
import com.nethcare.repository.OrderRepository;
import com.nethcare.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * The order pipeline and the rules the client confirmed for it.
 *
 * Everything that changes an order goes through here rather than through the
 * controller, because each of these rules is a decision the system makes, not
 * something the screen can be trusted to enforce:
 *
 *   price      frozen on the line item when the order is created
 *   advance    40% of the bill before the order is allowed to go to the lab
 *   stock      reserved on creation, deducted on collection
 *   low stock  below the reorder level, flagged with nobody checking
 *   cancel     releases the reservation and writes a credit note
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final BillRepository bills;
    private final PaymentRepository payments;
    private final StockService stock;

    @Value("${nethcare.order.advance-percentage:40}")
    private BigDecimal advancePercent;

    @Value("${nethcare.followup.regular-months:12}")
    private int followUpMonths;

    public OrderService(OrderRepository orders, BillRepository bills,
                        PaymentRepository payments, StockService stock) {
        this.orders = orders;
        this.bills = bills;
        this.payments = payments;
        this.stock = stock;
    }

    // ------------------------------------------------------------- creating

    /**
     * Places an order and reserves everything on it.
     *
     * Reservation is part of creating the order, not a separate step somebody
     * can forget — an order that exists without a reservation would let the
     * same frame be promised twice.
     */
    @Transactional
    public Order place(Order order, List<OrderItem> lines, String placedBy) {
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException("An order needs at least one item.");
        }
        if (order.getCustomerName() == null || order.getCustomerName().isBlank()) {
            throw new BusinessException("An order needs a customer name.");
        }

        order.setOrderNo(nextOrderNo());
        order.setPlacedBy(placedBy);
        order.setStatus(OrderStatus.PLACED);

        for (OrderItem line : lines) {
            if (line.getQuantity() <= 0) {
                throw new BusinessException("Every order line needs a quantity of at least 1.");
            }
            StockItem item = stock.get(line.getStockItemId());

            // Price and description are copied from the catalogue now, and
            // never read from it again.
            line.setItemCode(item.getItemCode());
            line.setDescription(item.getBrand() == null || item.getBrand().isBlank()
                    ? item.getName()
                    : item.getBrand() + " " + item.getName());
            if (line.getUnitPrice() == null || line.getUnitPrice().compareTo(BigDecimal.ZERO) == 0) {
                line.setUnitPrice(item.getUnitPrice());
            }
            order.addItem(line);
        }

        Order saved = orders.save(order);
        log.info("Order {} placed for {} with {} line(s)",
                saved.getOrderNo(), saved.getCustomerName(), saved.getItems().size());

        for (OrderItem line : saved.getItems()) {
            stock.reserve(line.getStockItemId(), line.getQuantity());
        }
        return saved;
    }

    // ---------------------------------------------------------------- status

    /**
     * Moves an order one step along.
     *
     * The lab step is the gated one: the client will not send a customer to a
     * lab company without a deposit, so the advance is checked here rather
     * than trusted to the person at the counter.
     */
    @Transactional
    public Order advance(Long id) {
        Order order = get(id);
        if (!order.getStatus().isOpen()) {
            throw new BusinessException("Order " + order.getOrderNo() + " is already "
                    + order.getStatus().label().toLowerCase() + ".");
        }

        OrderStatus next = order.getStatus().next();
        if (next == null) {
            throw new BusinessException("Order " + order.getOrderNo() + " has no further step.");
        }

        if (next == OrderStatus.LAB) {
            requireAdvancePaid(order);
        }

        order.setStatus(next);
        Order saved = orders.save(order);

        if (next == OrderStatus.COLLECTED) {
            // The item actually leaves the shop here, not when it was ordered.
            for (OrderItem line : saved.getItems()) {
                stock.deduct(line.getStockItemId(), line.getQuantity());
            }
            log.info("Order {} collected, stock deducted", saved.getOrderNo());
        }
        return saved;
    }

    /**
     * The 40% rule. Refuses to let an order reach the lab without it, and
     * says how much is short so the counter knows what to ask for.
     */
    private void requireAdvancePaid(Order order) {
        Bill bill = bills.findByOrderId(order.getId()).orElse(null);
        if (bill == null) {
            throw new BusinessException("Order " + order.getOrderNo()
                    + " has no bill yet, so it cannot go to the lab.");
        }
        if (bill.isCancelled()) {
            throw new BusinessException("Order " + order.getOrderNo() + " has a cancelled bill.");
        }
        if (!bill.meetsAdvance(advancePercent)) {
            BigDecimal required = bill.getTotal()
                    .multiply(advancePercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            throw new BusinessException("Order " + order.getOrderNo() + " needs a "
                    + advancePercent.toPlainString() + "% advance of LKR " + required.toPlainString()
                    + " before it goes to the lab. LKR " + bill.getPaid().toPlainString()
                    + " paid so far.");
        }
    }

    /**
     * Cancels an order, puts the reserved stock back, and writes a credit note
     * if any money was taken.
     */
    @Transactional
    public Order cancel(Long id, String reason) {
        Order order = get(id);
        if (!order.getStatus().canBeCancelled()) {
            throw new BusinessException("Order " + order.getOrderNo()
                    + " is already " + order.getStatus().label().toLowerCase()
                    + " and cannot be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            order.setRemarks(order.getRemarks() == null
                    ? "Cancelled: " + reason
                    : order.getRemarks() + " | Cancelled: " + reason);
        }
        Order saved = orders.save(order);

        for (OrderItem line : saved.getItems()) {
            stock.release(line.getStockItemId(), line.getQuantity());
        }
        log.info("Order {} cancelled, {} line(s) released back to stock", saved.getOrderNo(), saved.getItems().size());

        bills.findByOrderId(saved.getId()).ifPresent(bill -> {
            if (!bill.isCancelled() && bill.getPaid().compareTo(BigDecimal.ZERO) > 0) {
                raiseCreditNote(bill, reason);
            } else if (!bill.isCancelled()) {
                bill.setCancelled(true);
                bills.save(bill);
            }
        });
        return saved;
    }

    /** Refund paperwork. Recorded, not deleted — the money did move. */
    private void raiseCreditNote(Bill bill, String reason) {
        bill.setCancelled(true);
        bill.setCreditNote(true);
        bill.setCreditNoteNo("CN-" + bill.getBillNo().replace("INV-", ""));
        bills.save(bill);
        log.info("Credit note {} raised for {}{}",
                bill.getCreditNoteNo(), bill.getBillNo(),
                reason == null ? "" : " (" + reason + ")");
    }

    // ---------------------------------------------------------------- billing

    /**
     * Raises the bill for an order: subtotal from the lines, then discount,
     * then any urgent surcharge, then the total.
     *
     * Money rounds to two decimals at every step. Working in exact decimals
     * throughout avoids the rounding drift that shows up as a bill being one
     * cent out when the customer adds it up.
     */
    @Transactional
    public Bill generateBill(Long orderId, BigDecimal discount, LocalDate followUpOn) {
        Order order = get(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order " + order.getOrderNo() + " is cancelled.");
        }
        if (bills.findByOrderId(orderId).isPresent()) {
            throw new BusinessException("Order " + order.getOrderNo() + " already has a bill.");
        }

        BigDecimal subtotal = money(order.subtotal());

        BigDecimal off = discount == null ? BigDecimal.ZERO : money(discount);
        if (off.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("A discount cannot be negative.");
        }
        BigDecimal afterDiscount = subtotal.subtract(off);
        if (afterDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("A discount of LKR " + off.toPlainString()
                    + " is more than the order total of LKR " + subtotal.toPlainString() + ".");
        }

        BigDecimal surcharge = BigDecimal.ZERO;
        if (order.getPriority() == OrderPriority.URGENT) {
            surcharge = money(afterDiscount.multiply(
                    BigDecimal.valueOf(order.getPriority().surchargePercent()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        Bill bill = new Bill();
        bill.setBillNo(nextBillNo());
        bill.setOrderId(orderId);
        bill.setSubtotal(subtotal);
        bill.setDiscount(off);
        bill.setSurcharge(surcharge);
        bill.setTotal(money(afterDiscount.add(surcharge)));
        bill.setPaid(BigDecimal.ZERO);
        bill.setFollowUpOn(followUpOn != null ? followUpOn
                : LocalDate.now().plusMonths(followUpMonths));

        Bill saved = bills.save(bill);
        log.info("Bill {} raised for order {}: subtotal {} discount {} surcharge {} total {}",
                saved.getBillNo(), order.getOrderNo(), subtotal, off, surcharge, saved.getTotal());
        return saved;
    }

    /**
     * Records money at the counter and brings the bill's running total up.
     *
     * Overpayment is refused rather than silently absorbed — if the customer
     * hands over a note for a smaller balance the staff member needs to know
     * before the change is given.
     */
    @Transactional
    public Payment takePayment(Long billId, BigDecimal amount, PaymentMethod method,
                              boolean isAdvance, String takenBy) {
        Bill bill = getBill(billId);
        if (bill.isCancelled()) {
            throw new BusinessException("Bill " + bill.getBillNo() + " is cancelled.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Enter a payment amount greater than zero.");
        }

        BigDecimal value = money(amount);
        BigDecimal balance = bill.balance();
        if (value.compareTo(balance) > 0) {
            throw new BusinessException("That is more than the balance of LKR "
                    + balance.toPlainString() + " on bill " + bill.getBillNo() + ".");
        }

        Payment payment = new Payment(billId, nextReceiptNo(), value,
                method == null ? PaymentMethod.CASH : method, isAdvance, takenBy);
        Payment saved = payments.save(payment);

        bill.setPaid(money(bill.getPaid().add(value)));
        bills.save(bill);

        log.info("Receipt {} — LKR {} {} on bill {} by {}", saved.getReceiptNo(), value,
                saved.getMethod().label().toLowerCase(), bill.getBillNo(), takenBy);
        return saved;
    }

    // ---------------------------------------------------------------- reading

    public Order get(Long id) {
        return orders.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No order with id " + id));
    }

    public Order byNumber(String orderNo) {
        return orders.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResourceNotFoundException("No order numbered " + orderNo));
    }

    public Bill getBill(Long id) {
        return bills.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No bill with id " + id));
    }

    public Bill billFor(Long orderId) {
        return bills.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No bill for order " + orderId));
    }

    public List<Order> all() {
        return orders.findAll();
    }

    /** Open orders — anything not yet collected or cancelled. */
    public List<Order> open() {
        return orders.findByStatusNotInOrderByIdDesc(
                List.of(OrderStatus.COLLECTED, OrderStatus.CANCELLED));
    }

    public List<Order> byStatus(OrderStatus status) {
        return orders.findByStatusOrderByIdDesc(status);
    }

    public List<Payment> paymentsFor(Long billId) {
        return payments.findByBillIdOrderByIdAsc(billId);
    }

    /** Orders past their promised date and still not collected. */
    public List<Order> overdue() {
        return orders.findByPromisedOnBeforeAndStatusNotIn(
                LocalDate.now(), List.of(OrderStatus.COLLECTED, OrderStatus.CANCELLED));
    }

    public long countByStatus(OrderStatus status) {
        return orders.countByStatus(status);
    }

    // ---------------------------------------------------------------- numbers

    /**
     * Order numbers count up rather than random, because staff read them out
     * loud over the phone and customers quote them back.
     */
    private String nextOrderNo() {
        long n = orders.count() + 1;
        String candidate = String.format("ORD-%04d", n);
        while (orders.findByOrderNo(candidate).isPresent()) {
            n++;
            candidate = String.format("ORD-%04d", n);
        }
        return candidate;
    }

    private String nextBillNo() {
        long n = bills.count() + 1;
        String candidate = String.format("INV-%04d", n);
        while (bills.findByBillNo(candidate).isPresent()) {
            n++;
            candidate = String.format("INV-%04d", n);
        }
        return candidate;
    }

    private String nextReceiptNo() {
        return String.format("RCP-%04d", payments.count() + 1);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
