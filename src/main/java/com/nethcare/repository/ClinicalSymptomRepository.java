package com.nethcare.repository;

import com.nethcare.model.ClinicalSymptom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicalSymptomRepository extends JpaRepository<ClinicalSymptom, Long> {
    Optional<ClinicalSymptom> findByExaminationId(Long examinationId);
}
