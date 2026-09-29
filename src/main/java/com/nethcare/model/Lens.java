package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import java.math.BigDecimal;

/** Lens-specific catalogue data attached to one stock item. */
@Entity
@Table(name = "lenses")
public class Lens extends BaseEntity {
    @Column(name = "stock_item_id", nullable = false, unique = true)
    private Long stockItemId;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_item_id", insertable = false, updatable = false)
    private StockItem stockItem;
    @Column(name = "lens_type", nullable = false, length = 50)
    private String lensType;
    @Column(precision = 4, scale = 1)
    private BigDecimal diameter;
    @Column(length = 30)
    private String colour;
    @Column(length = 50)
    private String segment;
    @Column(name = "uv_coat", nullable = false)
    private boolean uvCoat;
    @Column(name = "anti_reflective_coat", nullable = false)
    private boolean antiReflectiveCoat;

    public Long getStockItemId() { return stockItemId; }
    public void setStockItemId(Long stockItemId) { this.stockItemId = stockItemId; }
    public String getLensType() { return lensType; }
    public void setLensType(String lensType) { this.lensType = lensType; }
    public BigDecimal getDiameter() { return diameter; }
    public void setDiameter(BigDecimal diameter) { this.diameter = diameter; }
    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
    public boolean isUvCoat() { return uvCoat; }
    public void setUvCoat(boolean uvCoat) { this.uvCoat = uvCoat; }
    public boolean isAntiReflectiveCoat() { return antiReflectiveCoat; }
    public void setAntiReflectiveCoat(boolean antiReflectiveCoat) { this.antiReflectiveCoat = antiReflectiveCoat; }
}
