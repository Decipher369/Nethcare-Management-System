package com.nethcare.repository;

import com.nethcare.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    boolean existsByExaminationId(Long examinationId);
    Optional<Referral> findByExaminationId(Long examinationId);

    List<Referral> findByPatientIdOrderByReferredOnDesc(Long patientId);

    // The surgeon's worklist — referred patients, newest first.
    List<Referral> findByStatusOrderByReferredOnDesc(String status);

    List<Referral> findAllByOrderByReferredOnDesc();

    List<Referral> findBySurgeonNameOrderByReferredOnDesc(String surgeonName);

    List<Referral> findBySurgeonNameAndStatusOrderByReferredOnDesc(String surgeonName, String status);
}
