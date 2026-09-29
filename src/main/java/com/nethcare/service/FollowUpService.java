package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.AuditAction;
import com.nethcare.model.FollowUp;
import com.nethcare.model.FollowUpOutcome;
import com.nethcare.model.FollowUpStatus;
import com.nethcare.model.Notification;
import com.nethcare.model.NotificationChannel;
import com.nethcare.model.NotificationStatus;
import com.nethcare.repository.FollowUpRepository;
import com.nethcare.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The follow-up rules and the reminder queue.
 *
 *   due  = last examination + 12 months, or + 6 for a contact lens patient
 *   SMS first, email as the fallback
 *   an opted-out patient is never queued
 *
 * Building the list from real examinations is the part that waits for the
 * merge — M2 owns the examinations table and it is not in this branch. Until
 * then the rows are seeded by hand and every method below works normally on
 * them, so the rules can be tested without the rest of the system.
 */
@Service
public class FollowUpService {

    private final FollowUpRepository followUps;
    private final NotificationRepository notifications;
    private final AuditService audit;

    @Value("${nethcare.followup.regular-months:12}")
    private int regularMonths;

    @Value("${nethcare.followup.contact-lens-months:6}")
    private int contactLensMonths;

    public FollowUpService(FollowUpRepository followUps,
                           NotificationRepository notifications,
                           AuditService audit) {
        this.followUps = followUps;
        this.notifications = notifications;
        this.audit = audit;
    }

    /** Everyone still waiting for an answer, most overdue first. */
    @Transactional(readOnly = true)
    public List<FollowUp> open() {
        return followUps.findByStatusInOrderByDueOnAsc(List.of(FollowUpStatus.PENDING));
    }

    @Transactional(readOnly = true)
    public List<FollowUp> forPatient(Long patientId) {
        return followUps.findByPatientIdOrderByDueOnAsc(patientId);
    }

    @Transactional(readOnly = true)
    public FollowUp get(Long id) {
        return followUps.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No follow-up with id " + id));
    }

    /**
     * The date a patient falls due, from the examination date. Kept here
     * rather than in the entity so the 12 and 6 stay configurable and the
     * rule has one home.
     */
    public LocalDate dueOn(LocalDate lastExamOn, boolean contactLensUser) {
        int months = contactLensUser ? contactLensMonths : regularMonths;
        return lastExamOn.plusMonths(months);
    }

    public String dueForWho(boolean contactLensUser) {
        return (contactLensUser ? contactLensMonths : regularMonths) + " months";
    }

    /**
     * Queues the reminder for one patient. SMS wins when there is a number,
     * email is the fallback, and an opted-out patient gets nothing queued at
     * all rather than a row that is silently ignored later.
     */
    @Transactional
    public Notification notify(Long followUpId, String actor) {
        FollowUp f = get(followUpId);
        if (Boolean.TRUE.equals(f.getOptOut())) {
            throw new BusinessException("Patient " + f.getPatientId()
                    + " has opted out of reminders, so nothing was queued.");
        }

        boolean hasPhone = f.getPhone() != null && !f.getPhone().isBlank();
        NotificationChannel channel = hasPhone
                ? NotificationChannel.SMS
                : NotificationChannel.EMAIL;
        String destination = hasPhone ? f.getPhone() : f.getEmail();

        if (destination == null || destination.isBlank()) {
            // No way to reach them. Recorded as failed rather than left queued,
            // because a queued row with nowhere to go just hides the problem.
            Notification dead = build(f, NotificationChannel.SMS, null,
                    "No phone or email on file", NotificationStatus.FAILED);
            dead.setFailureReason("No contact details on file");
            notifications.save(dead);
            throw new BusinessException("Patient " + f.getPatientId()
                    + " has no contact details, so the reminder could not be queued.");
        }

        Notification saved = build(f, channel, destination, null, NotificationStatus.QUEUED);
        notifications.save(saved);
        audit.record(actor, AuditAction.CREATE, "Notification",
                saved.getReference(), null, channel.label() + " -> " + destination);
        return saved;
    }

    private Notification build(FollowUp f, NotificationChannel channel, String destination,
                               String skipped, NotificationStatus status) {
        Notification n = new Notification();
        n.setReference(nextReference());
        n.setFollowUpId(f.getId());
        n.setPatientId(f.getPatientId());
        n.setPatientName(f.getPatientName());
        n.setChannel(channel);
        n.setDestination(destination);
        n.setMessage("Nethcare: you are due for a review. Reply to book an appointment.");
        n.setStatus(status);
        n.setSkippedReason(skipped);
        n.setScheduledFor(LocalDateTime.now());
        return n;
    }

    /**
     * Records what the patient said. A booking moves the row to BOOKED, and
     * the date they chose is kept so the front desk can see it without
     * opening M1.
     */
    @Transactional
    public FollowUp recordResponse(Long followUpId, FollowUpOutcome outcome,
                                   String note, LocalDate bookedOn, String actor) {
        FollowUp f = get(followUpId);
        f.setOutcome(outcome);
        f.setResponseNote(note);
        f.setRespondedOn(LocalDate.now());

        if (outcome == FollowUpOutcome.BOOKED) {
            f.setBookedOn(bookedOn == null ? LocalDate.now() : bookedOn);
            f.setStatus(FollowUpStatus.BOOKED);
        } else if (outcome == FollowUpOutcome.DECLINED) {
            f.setStatus(FollowUpStatus.CLOSED);
        } else {
            // No answer or a dead number: the row stays open so tomorrow's
            // worklist still has it. A customer who never picked up has not
            // been told no.
            f.setStatus(FollowUpStatus.PENDING);
        }

        FollowUp saved = followUps.save(f);
        audit.record(actor, AuditAction.UPDATE, "FollowUp",
                String.valueOf(followUpId), "PENDING", outcome.name());
        return saved;
    }

    /** Marks a queued reminder as handed over. There is no SMS gateway here. */
    @Transactional
    public Notification markSent(Long id, String actor) {
        Notification n = notifications.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No notification with id " + id));
        if (!n.isQueued()) {
            throw new BusinessException("Notification " + n.getReference()
                    + " is already " + n.getStatus().label().toLowerCase() + ".");
        }
        n.setStatus(NotificationStatus.SENT);
        n.setSentAt(LocalDateTime.now());
        Notification saved = notifications.save(n);
        audit.record(actor, AuditAction.UPDATE, "Notification",
                saved.getReference(), "QUEUED", "SENT");
        return saved;
    }

    /**
     * RCP-0001 style counter, off the highest existing number so a deleted row
     * does not hand the same reference out twice.
     */
    private String nextReference() {
        long max = notifications.findAll().stream()
                .map(Notification::getReference)
                .filter(r -> r != null && r.startsWith("RCP-"))
                .mapToLong(r -> Long.parseLong(r.substring(4)))
                .max()
                .orElse(0L);
        return String.format("RCP-%04d", max + 1);
    }
}
