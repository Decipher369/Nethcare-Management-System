package com.nethcare.repository;

import com.nethcare.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /** Everything an order has claimed, for the release-on-cancel sweep. */
    List<OrderItem> findByOrderIdAndStockItemId(Long orderId, Long stockItemId);
}
