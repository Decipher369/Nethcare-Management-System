package com.nethcare.repository;

import com.nethcare.model.ContactLensTracker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ContactLensTrackerRepository extends JpaRepository<ContactLensTracker, Long> {
    List<ContactLensTracker> findByPatientIdOrderByReplacementDateDesc(Long patientId);
    List<ContactLensTracker> findByAlertSentFalseAndReplacementDateLessThanEqual(LocalDate date);
}
