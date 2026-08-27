package com.nethcare.repository;

import com.nethcare.model.Order;
import com.nethcare.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);

    List<Order> findByStatusOrderByIdDesc(OrderStatus status);

    List<Order> findByCustomerNameContainingIgnoreCaseOrderByIdDesc(String name);

    /** The counter's "open orders" list — anything not collected or cancelled. */
    List<Order> findByStatusNotInOrderByIdDesc(List<OrderStatus> statuses);

    /** Orders past their promised date and still not collected. */
    List<Order> findByPromisedOnBeforeAndStatusNotIn(LocalDate date, List<OrderStatus> statuses);

    long countByStatus(OrderStatus status);
}
