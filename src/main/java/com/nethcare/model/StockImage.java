package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** Uploaded catalogue photos live separately so stock queries do not load image bytes. */
@Entity
@Table(name = "stock_images")
public class StockImage {
    @Id
    @Column(length = 64)
    private String name;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    protected StockImage() { }

    public StockImage(String name, String contentType, byte[] data) {
        this.name = name;
        this.contentType = contentType;
        this.data = data;
    }

    public String getName() { return name; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
}
