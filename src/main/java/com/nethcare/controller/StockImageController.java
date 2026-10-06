package com.nethcare.controller;

import com.nethcare.service.StockImageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class StockImageController {
    private final StockImageService images;

    public StockImageController(StockImageService images) {
        this.images = images;
    }

    @GetMapping("/images/stock/{name}")
    public ResponseEntity<byte[]> photo(@PathVariable String name) {
        return images.find(name)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.getContentType()))
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                        .body(image.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
