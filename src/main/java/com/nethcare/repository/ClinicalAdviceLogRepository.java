package com.nethcare.repository;

import com.nethcare.model.ClinicalAdviceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClinicalAdviceLogRepository extends JpaRepository<ClinicalAdviceLog, Long> {
    Optional<ClinicalAdviceLog> findTopByOrderByIdDesc();
    List<ClinicalAdviceLog> findByPatientIdOrderByRecordedAtUtcDesc(Long patientId);
}
