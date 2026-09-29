package com.nethcare.repository;

import com.nethcare.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByStockItemIdOrderByRecordedAtDesc(Long stockItemId);
    List<StockMovement> findByOrderIdOrderByRecordedAtAsc(Long orderId);
}
