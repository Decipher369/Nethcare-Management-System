package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/** Frame-specific catalogue data attached to one stock item. */
@Entity
@Table(name = "frames")
public class Frame extends BaseEntity {
    @Column(name = "stock_item_id", nullable = false, unique = true)
    private Long stockItemId;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_item_id", insertable = false, updatable = false)
    private StockItem stockItem;
    @Column(nullable = false, length = 50)
    private String model;
    @Column(length = 50)
    private String brand;
    @Column(length = 20)
    private String size;
    @Column(length = 30)
    private String colour;

    public Long getStockItemId() { return stockItemId; }
    public void setStockItemId(Long stockItemId) { this.stockItemId = stockItemId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }
}
