package com.nethcare.repository;

import com.nethcare.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    List<Referral> findByPatientIdOrderByReferredOnDesc(Long patientId);

    // The surgeon's worklist — referred patients, newest first.
    List<Referral> findByStatusOrderByReferredOnDesc(String status);

    List<Referral> findAllByOrderByReferredOnDesc();
}
