package com.nethcare.repository;

import com.nethcare.model.Lens;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LensRepository extends JpaRepository<Lens, Long> {
    Optional<Lens> findByStockItemId(Long stockItemId);
}
