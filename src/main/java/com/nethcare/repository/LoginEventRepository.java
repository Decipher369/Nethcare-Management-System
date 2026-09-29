package com.nethcare.repository;

import com.nethcare.model.LoginEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {
    List<LoginEvent> findTop200ByOrderByOccurredAtDesc();
    List<LoginEvent> findTop100ByUserIdOrderByOccurredAtDesc(Long userId);
}
