package com.nethcare.repository;

import com.nethcare.model.Examination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExaminationRepository extends JpaRepository<Examination, Long> {

    // Newest first — a prescription history that reads oldest first is the one
    // the optician has to scroll past every time.
    List<Examination> findByPatientIdOrderByExamDateDesc(Long patientId);

    List<Examination> findTop4ByPatientIdOrderByExamDateDesc(Long patientId);
}
