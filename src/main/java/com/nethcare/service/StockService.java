package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.StockCategory;
import com.nethcare.model.StockItem;
import com.nethcare.repository.StockItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything that changes a stock count.
 *
 * The three movements the deck lists are kept apart on purpose:
 *   reserve  — an order is placed, the item is set aside but still on the shelf
 *   deduct   — the customer collects, the item actually leaves
 *   release  — the order is cancelled, the reservation goes back
 *
 * Mixing them up is how a shop ends up promising a frame it has already sold,
 * so each one is its own method and the arithmetic lives in one place.
 */
@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final StockItemRepository items;

    public StockService(StockItemRepository items) {
        this.items = items;
    }

    // ---------------------------------------------------------------- reading

    public List<StockItem> listed() {
        return items.findByIsActiveTrueOrderByCategoryAscNameAsc();
    }

    /** Gallery view. Only items the counter has marked as listed are shown. */
    public List<StockItem> gallery(StockCategory category, String query) {
        return search(category, query);
    }

    public List<StockItem> search(StockCategory category, String query) {
        return items.searchListed(
                category,
                (query == null || query.isBlank()) ? null : query.trim());
    }

    public List<StockItem> lowStock() {
        return items.findLowStock();
    }

    public StockItem get(Long id) {
        return items.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No stock item with id " + id));
    }

    public StockItem byCode(String code) {
        return items.findByItemCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No stock item with code " + code));
    }

    // ---------------------------------------------------------------- writing

    @Transactional
    public StockItem add(StockItem item) {
        if (items.findByItemCode(item.getItemCode()).isPresent()) {
            throw new BusinessException("Stock code " + item.getItemCode() + " already exists.");
        }
        if (item.getQuantity() < 0) {
            throw new BusinessException("Quantity cannot be negative.");
        }
        item.setReserved(0);
        StockItem saved = items.save(item);
        log.info("Stock added: {} x{} at {}", saved.getItemCode(), saved.getQuantity(), saved.getUnitPrice());
        return saved;
    }

    @Transactional
    public StockItem update(Long id, StockItem incoming) {
        StockItem item = get(id);
        if (incoming.getUnitPrice() != null) {
            item.setUnitPrice(incoming.getUnitPrice());
        }
        if (incoming.getName() != null) {
            item.setName(incoming.getName());
        }
        if (incoming.getBrand() != null) {
            item.setBrand(incoming.getBrand());
        }
        if (incoming.getDescription() != null) {
            item.setDescription(incoming.getDescription());
        }
        if (incoming.getImageName() != null) {
            item.setImageName(incoming.getImageName());
        }
        if (incoming.getReorderLevel() >= 0) {
            item.setReorderLevel(incoming.getReorderLevel());
        }
        StockItem saved = items.save(item);
        log.info("Stock updated: {}", saved.getItemCode());
        return saved;
    }

    /**
     * Counter recount. The staff member is stating what is actually on the
     * shelf, so the number they type wins outright.
     *
     * The reservation is left alone — a recount must not silently hand an
     * open order's frame to somebody else. If the new count is below what is
     * already reserved, that is a real problem and the caller has to resolve
     * it rather than have it papered over.
     */
    @Transactional
    public StockItem adjustQuantity(Long id, int counted, String reason) {
        StockItem item = get(id);
        if (counted < 0) {
            throw new BusinessException("Counted quantity cannot be negative.");
        }
        if (counted < item.getReserved()) {
            throw new BusinessException(
                    "Only " + item.getReserved() + " " + item.getItemCode()
                            + " are reserved for open orders, so the count cannot be " + counted + ".");
        }
        int before = item.getQuantity();
        item.setQuantity(counted);
        StockItem saved = items.save(item);
        log.info("Stock adjusted: {} {} -> {} ({})", saved.getItemCode(), before, counted, reason);
        return saved;
    }

    // ------------------------------------------------------------- movements

    /**
     * Set the item aside for an order. Nothing leaves the shelf yet, so this
     * only moves the reserved count.
     */
    @Transactional
    public StockItem reserve(Long id, int qty) {
        StockItem item = get(id);
        if (qty <= 0) {
            throw new BusinessException("Reserve at least one item.");
        }
        if (item.available() < qty) {
            throw new BusinessException(
                    "Only " + item.available() + " " + item.getItemCode() + " available.");
        }
        item.setReserved(item.getReserved() + qty);
        StockItem saved = items.save(item);
        log.info("Reserved {} x{} for an order", qty, saved.getItemCode());
        return saved;
    }

    /** Collection. The item leaves the shelf and the reservation clears. */
    @Transactional
    public StockItem deduct(Long id, int qty) {
        StockItem item = get(id);
        if (qty <= 0) {
            throw new BusinessException("Deduct at least one item.");
        }
        if (item.getQuantity() < qty) {
            throw new BusinessException("Cannot deduct " + qty + " " + item.getItemCode()
                    + " — only " + item.getQuantity() + " on the shelf.");
        }
        int toRelease = Math.min(qty, item.getReserved());
        item.setQuantity(item.getQuantity() - qty);
        item.setReserved(item.getReserved() - toRelease);
        StockItem saved = items.save(item);
        log.info("Deducted {} x{} on collection", qty, saved.getItemCode());
        return saved;
    }

    /** Cancellation. The reservation goes back and the shelf is untouched. */
    @Transactional
    public StockItem release(Long id, int qty) {
        StockItem item = get(id);
        if (qty <= 0) {
            throw new BusinessException("Release at least one item.");
        }
        if (item.getReserved() < qty) {
            throw new BusinessException(
                    "Only " + item.getReserved() + " " + item.getItemCode() + " are reserved.");
        }
        item.setReserved(item.getReserved() - qty);
        StockItem saved = items.save(item);
        log.info("Released {} x{} back to available stock", qty, saved.getItemCode());
        return saved;
    }

    // -------------------------------------------------------------- warnings

    /** Expired, or expiring within a month. Contact lens stock mainly. */
    public List<StockItem> expiringSoon() {
        LocalDate cutoff = LocalDate.now().plusMonths(1);
        return listed().stream()
                .filter(i -> i.getExpiresOn() != null && !i.getExpiresOn().isAfter(cutoff))
                .toList();
    }

    public List<StockItem> byCategory(StockCategory category) {
        return items.findByCategoryOrderByNameAsc(category);
    }
}
