package com.nethcare.repository;

import com.nethcare.model.Frame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FrameRepository extends JpaRepository<Frame, Long> {
    Optional<Frame> findByStockItemId(Long stockItemId);
}
