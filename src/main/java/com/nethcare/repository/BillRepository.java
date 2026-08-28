package com.nethcare.repository;

import com.nethcare.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillNo(String billNo);

    Optional<Bill> findByOrderId(Long orderId);

    List<Bill> findByOrderIdIn(List<Long> orderIds);

    /** "Quick bill lookup" in the client spec (FR-4.4) — type any part of the bill number. */
    List<Bill> findByBillNoContainingIgnoreCaseOrderByIdDesc(String fragment);

    List<Bill> findByFollowUpOnBetweenAndCancelledFalse(LocalDate from, LocalDate to);

    List<Bill> findByCreatedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);
}
