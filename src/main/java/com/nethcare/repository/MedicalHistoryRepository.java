package com.nethcare.repository;

import com.nethcare.model.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {
    List<MedicalHistory> findByPatientIdOrderByVersionNumberDesc(Long patientId);
    Optional<MedicalHistory> findFirstByPatientIdOrderByVersionNumberDesc(Long patientId);
    Optional<MedicalHistory> findByExaminationId(Long examinationId);
    long countByPatientId(Long patientId);
}
