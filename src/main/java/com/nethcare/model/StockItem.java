package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Anything Nethcare sells: a frame, a lens, a contact lens pack, a case.
 *
 * One table for all of them, because the counter treats them the same way —
 * it counts them, prices them and gets alerts when they run low. The
 * category decides which extra fields matter: a frame has a model and a
 * picture, a contact lens has an expiry date.
 *
 * quantity is what is on the shelf. Reserved is what is spoken for by an
 * order that has not been collected yet, so the two are tracked apart.
 */
@Entity
@Table(name = "stock_items")
public class StockItem extends BaseEntity {

    /** The code the counter looks a product up by, e.g. TR-204. */
    @Column(name = "item_code", nullable = false, unique = true, length = 40)
    private String itemCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 80)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockCategory category;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** On the shelf now. Drops when an order is collected. */
    @Column(nullable = false)
    private int quantity = 0;

    /** Set aside for an open order. Goes back if the order is cancelled. */
    @Column(nullable = false)
    private int reserved = 0;

    /** Once free stock falls below this, the item shows on the low-stock list. */
    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 0;

    /** Contact lenses and some solutions go past a date. Frames do not. */
    @Column(name = "expires_on")
    private LocalDate expiresOn;

    /** Uploaded photo key, or a legacy file name under /static/images/frames. */
    @Column(name = "image_name", length = 200)
    private String imageName;

    @Column(length = 500)
    private String description;

    /**
     * On the shelf and not spoken for. This is the number a visitor should
     * never see directly — see StockService.isLow.
     */
    public int available() {
        return Math.max(0, quantity - reserved);
    }

    public boolean isLow() {
        return available() <= reorderLevel;
    }

    /** Frames can be listed before the counter has received them yet. */
    public boolean isListed() {
        return getIsActive();
    }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public StockCategory getCategory() { return category; }
    public void setCategory(StockCategory category) { this.category = category; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getReserved() { return reserved; }
    public void setReserved(int reserved) { this.reserved = reserved; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }

    public String getImageUrl() {
        if (imageName == null || imageName.isBlank()) return null;
        return (imageName.startsWith("upload-") ? "/images/stock/" : "/images/frames/") + imageName;
    }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
