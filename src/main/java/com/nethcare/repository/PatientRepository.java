package com.nethcare.repository;

import com.nethcare.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientNo(String patientNo);

    Optional<Patient> findByUserId(Long userId);

    // One query for the search box. Starts with a leading % because the user
    // usually types the end of a name, not the start. Trigram index would be
    // the fix once the table outgrows a few thousand rows.
    @Query("""
            select p from Patient p
            where lower(p.fullName) like lower(concat('%', :term, '%'))
               or lower(coalesce(p.phone, '')) like lower(concat('%', :term, '%'))
               or lower(p.patientNo) like lower(concat('%', :term, '%'))
            order by p.fullName
            """)
    List<Patient> search(@Param("term") String term);

    List<Patient> findAllByOrderByFullNameAsc();
}
