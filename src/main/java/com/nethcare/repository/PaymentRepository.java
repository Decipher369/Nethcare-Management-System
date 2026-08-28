package com.nethcare.repository;

import com.nethcare.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBillIdOrderByIdAsc(Long billId);

    List<Payment> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
