package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

/** Append-only inventory ledger entry. */
@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_stock_movement_item_time", columnList = "stock_item_id, recorded_at"),
        @Index(name = "idx_stock_movement_order", columnList = "order_id")
})
public class StockMovement extends BaseEntity {
    @Column(name = "stock_item_id", nullable = false)
    private Long stockItemId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_item_id", insertable = false, updatable = false)
    private StockItem stockItem;
    @Column(name = "order_id")
    private Long orderId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType movementType;
    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;
    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;
    @Column(name = "reserved_after", nullable = false)
    private int reservedAfter;
    @Column(name = "recorded_by", nullable = false, length = 80)
    private String recordedBy;
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
    @Column(length = 500)
    private String reason;

    public Long getStockItemId() { return stockItemId; }
    public void setStockItemId(Long stockItemId) { this.stockItemId = stockItemId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public StockMovementType getMovementType() { return movementType; }
    public void setMovementType(StockMovementType movementType) { this.movementType = movementType; }
    public int getQuantityChange() { return quantityChange; }
    public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }
    public int getQuantityAfter() { return quantityAfter; }
    public void setQuantityAfter(int quantityAfter) { this.quantityAfter = quantityAfter; }
    public int getReservedAfter() { return reservedAfter; }
    public void setReservedAfter(int reservedAfter) { this.reservedAfter = reservedAfter; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
