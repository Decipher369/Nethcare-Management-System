package com.nethcare.repository;

import com.nethcare.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByRxNo(String rxNo);

    boolean existsByExaminationId(Long examinationId);
    Optional<Prescription> findByExaminationId(Long examinationId);

    List<Prescription> findByPatientIdOrderByIssuedOnDesc(Long patientId);

    List<Prescription> findTop4ByPatientIdOrderByIssuedOnDesc(Long patientId);
}
