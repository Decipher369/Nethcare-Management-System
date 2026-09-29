package com.nethcare.repository;

import com.nethcare.model.FollowUp;
import com.nethcare.model.FollowUpStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Follow-up worklists.
 *
 * Ordering is by due date so the most overdue sit at the top, and the filter
 * takes the status in a list because "open" means PENDING only — the operator
 * would otherwise have to know the enum name to ask for a worklist.
 */
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    List<FollowUp> findByPatientIdOrderByDueOnAsc(Long patientId);

    List<FollowUp> findByStatusInOrderByDueOnAsc(List<FollowUpStatus> statuses);

    List<FollowUp> findByStatusInAndDueOnLessThanEqualOrderByDueOnAsc(
            List<FollowUpStatus> statuses, LocalDate on);

    List<FollowUp> findAllByOrderByDueOnAsc();

    long countByStatusIn(List<FollowUpStatus> statuses);
}
