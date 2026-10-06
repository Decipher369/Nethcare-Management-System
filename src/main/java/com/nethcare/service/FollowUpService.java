package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.*;
import com.nethcare.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Implements the M4 review-list and reminder-dispatch workflows. */
@Service
public class FollowUpService {
    private static final List<FollowUpStatus> OPEN = List.of(
            FollowUpStatus.ACTIVE, FollowUpStatus.REVIEW_QUEUED, FollowUpStatus.NOTIFIED);

    private final FollowUpRepository followUps;
    private final NotificationRepository notifications;
    private final PatientRepository patients;
    private final UserRepository users;
    private final ExaminationRepository examinations;
    private final OrderRepository orders;
    private final AuditService audit;

    public FollowUpService(FollowUpRepository followUps, NotificationRepository notifications,
                           PatientRepository patients, UserRepository users,
                           ExaminationRepository examinations, OrderRepository orders, AuditService audit) {
        this.followUps = followUps;
        this.notifications = notifications;
        this.patients = patients;
        this.users = users;
        this.examinations = examinations;
        this.orders = orders;
        this.audit = audit;
    }

    @Transactional
    public FollowUp createCase(Long patientId, Long assignedOpticianId, Long originatingVisitId,
                               FollowUpCategory category, LocalDate targetReviewDate,
                               boolean highRisk, String clinicalNotes, String actor) {
        Patient patient = patients.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("No patient with id " + patientId));
        User optician = users.findById(assignedOpticianId)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + assignedOpticianId));
        if (optician.getRole() != Role.OPTICIAN && optician.getRole() != Role.ADMIN) {
            throw new BusinessException("The follow-up must be assigned to an optician or administrator.");
        }
        Examination visit = examinations.findById(originatingVisitId)
                .orElseThrow(() -> new ResourceNotFoundException("No examination with id " + originatingVisitId));
        if (!patientId.equals(visit.getPatientId())) {
            throw new BusinessException("The originating examination belongs to another patient.");
        }
        if (category == null || targetReviewDate == null) {
            throw new BusinessException("Category and target review date are required.");
        }

        FollowUp row = new FollowUp();
        row.setPatientId(patientId);
        row.setAssignedOpticianId(assignedOpticianId);
        row.setOriginatingVisitId(originatingVisitId);
        row.setPatientName(patient.getFullName());
        row.setPhone(patient.getPhone());
        row.setEmail(patient.getEmail());
        row.setLastExamOn(visit.getExamDate());
        row.setDueOn(targetReviewDate);
        row.setCategory(category);
        row.setHighRisk(highRisk);
        row.setResponseNote(clinicalNotes);
        row.setStatus(FollowUpStatus.ACTIVE);
        FollowUp saved = followUps.save(row);
        audit.record(actor, AuditAction.CREATE, "PatientFollowUpCase", saved.getId().toString(),
                null, category.name() + " due " + targetReviewDate);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<FollowUp> open() {
        return followUps.findByStatusInOrderByDueOnAsc(OPEN);
    }

    @Transactional(readOnly = true)
    public List<FollowUp> weeklyList(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BusinessException("A valid review date range is required.");
        }
        return followUps.findByStatusInAndDueOnBetweenOrderByHighRiskDescDueOnAsc(OPEN, from, to)
                .stream().filter(row -> row.getBookedOn() == null).toList();
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

    @Transactional
    public Notification notify(Long followUpId, String actor) {
        FollowUp f = get(followUpId);
        if (Boolean.TRUE.equals(f.getOptOut())) {
            return saveExcluded(f, "Patient opted out", actor);
        }
        String phone = normalizeSriLankanPhone(f.getPhone());
        if (phone == null) {
            Notification failed = build(f, f.getPhone(), NotificationStatus.FAILED);
            failed.setFailureReason("Missing or invalid Sri Lankan mobile number");
            Notification saved = notifications.save(failed);
            audit.record(actor, AuditAction.CREATE, "NotificationDispatch", saved.getReference(),
                    null, "FAILED: invalid phone");
            return saved;
        }
        Notification saved = notifications.save(build(f, phone, NotificationStatus.PENDING));
        f.setStatus(FollowUpStatus.REVIEW_QUEUED);
        followUps.save(f);
        audit.record(actor, AuditAction.CREATE, "NotificationDispatch", saved.getReference(),
                null, "PENDING -> " + phone);
        return saved;
    }

    @Transactional
    public int queueCohort(LocalDate from, LocalDate to, String actor) {
        int count = 0;
        for (FollowUp followUp : weeklyList(from, to)) {
            boolean alreadyOpen = notifications.existsByFollowUpIdAndStatusIn(followUp.getId(),
                    List.of(NotificationStatus.PENDING, NotificationStatus.RETRY_PENDING, NotificationStatus.SENT));
            if (!alreadyOpen) {
                notify(followUp.getId(), actor);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public Notification queueOrderReady(Long orderId, String actor) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No order with id " + orderId));
        if (order.getStatus() != OrderStatus.READY) {
            throw new BusinessException("Only a ready order can produce a pickup reminder.");
        }
        String phone = normalizeSriLankanPhone(order.getCustomerPhone());
        Notification n = new Notification();
        n.setReference(nextReference());
        n.setOrderId(orderId);
        n.setPatientName(order.getCustomerName());
        n.setType(NotificationType.ORDER_READY);
        n.setChannel(NotificationChannel.SMS);
        n.setDestination(phone == null ? order.getCustomerPhone() : phone);
        n.setMessage("Nethcare: " + order.getCustomerName() + ", order " + order.getOrderNo()
                + " is ready for pickup. Please contact the clinic if you need assistance.");
        n.setScheduledFor(LocalDateTime.now());
        n.setStatus(phone == null ? NotificationStatus.FAILED : NotificationStatus.PENDING);
        if (phone == null) n.setFailureReason("Missing or invalid Sri Lankan mobile number");
        Notification saved = notifications.save(n);
        audit.record(actor, AuditAction.CREATE, "NotificationDispatch", saved.getReference(),
                null, saved.getStatus() + " order=" + order.getOrderNo());
        return saved;
    }

    @Transactional
    public int queueNewReadyOrders(String actor) {
        int count = 0;
        for (Order order : orders.findByStatusOrderByIdDesc(OrderStatus.READY)) {
            if (!notifications.existsByOrderId(order.getId())) {
                queueOrderReady(order.getId(), actor);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public Notification sendPatientSms(Long patientId, String messageText, String actor) {
        Patient patient = patients.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("No patient with id " + patientId));
        String phone = normalizeSriLankanPhone(patient.getPhone());
        Notification n = new Notification();
        n.setReference(nextReference());
        n.setPatientId(patient.getId());
        if (patient.getUserId() != null) n.setRecipientUserId(patient.getUserId());
        n.setPatientName(patient.getFullName());
        n.setType(NotificationType.PATIENT_SMS);
        n.setChannel(NotificationChannel.SMS);
        n.setDestination(phone == null ? patient.getPhone() : phone);
        n.setMessage(messageText != null && !messageText.isBlank() ? messageText.trim() :
                ("Nethcare: " + patient.getFullName() + ", your follow-up appointment is scheduled at Nethcare Clinic."));
        n.setScheduledFor(LocalDateTime.now());
        n.setStatus(phone == null ? NotificationStatus.FAILED : NotificationStatus.PENDING);
        if (phone == null) {
            n.setFailureReason("Missing or invalid Sri Lankan mobile number");
        }
        Notification saved = notifications.save(n);
        audit.record(actor, AuditAction.CREATE, "NotificationDispatch", saved.getReference(),
                null, "PATIENT_SMS -> " + n.getDestination());
        return saved;
    }

    @Transactional
    public Notification sendDirectSms(String rawPhone, String recipientName, String messageText, String actor) {
        String phone = normalizeSriLankanPhone(rawPhone);
        Notification n = new Notification();
        n.setReference(nextReference());
        n.setPatientName(recipientName != null && !recipientName.isBlank() ? recipientName.trim() : "Ad-hoc Recipient");
        n.setType(NotificationType.PATIENT_SMS);
        n.setChannel(NotificationChannel.SMS);
        n.setDestination(phone == null ? rawPhone : phone);
        n.setMessage(messageText != null && !messageText.isBlank() ? messageText.trim() :
                ("Nethcare: " + n.getPatientName() + ", notification from Nethcare Clinic."));
        n.setScheduledFor(LocalDateTime.now());
        n.setStatus(phone == null ? NotificationStatus.FAILED : NotificationStatus.PENDING);
        if (phone == null) {
            n.setFailureReason("Missing or invalid Sri Lankan mobile number");
        }
        Notification saved = notifications.save(n);
        audit.record(actor, AuditAction.CREATE, "NotificationDispatch", saved.getReference(),
                null, "DIRECT_SMS -> " + n.getDestination());
        return saved;
    }

    @Transactional
    public Notification markSent(Long id, String gatewayReceiptId, String actor) {
        Notification n = notification(id);
        if (!n.isQueued()) throw new BusinessException("Notification " + n.getReference() + " is not pending.");
        if (gatewayReceiptId == null || gatewayReceiptId.isBlank()) {
            throw new BusinessException("The SMS gateway receipt id is required.");
        }
        n.setLastAttemptAt(LocalDateTime.now());
        n.setGatewayReceiptId(gatewayReceiptId.trim());
        n.setStatus(NotificationStatus.SENT);
        n.setSentAt(LocalDateTime.now());
        if (n.getFollowUpId() != null) {
            FollowUp f = get(n.getFollowUpId());
            f.setStatus(FollowUpStatus.NOTIFIED);
            followUps.save(f);
        }
        Notification saved = notifications.save(n);
        audit.record(actor, AuditAction.UPDATE, "NotificationDispatch", saved.getReference(),
                "PENDING", "SENT receipt=" + gatewayReceiptId);
        return saved;
    }

    /** Compatibility entry point for the existing console action. */
    @Transactional
    public Notification markSent(Long id, String actor) {
        return markSent(id, "MANUAL-" + id + "-" + System.currentTimeMillis(), actor);
    }

    @Transactional
    public Notification markDelivered(Long id, String actor) {
        Notification n = notification(id);
        if (n.getStatus() != NotificationStatus.SENT) {
            throw new BusinessException("Only a sent notification can be marked delivered.");
        }
        n.setStatus(NotificationStatus.DELIVERED);
        n.setDeliveredAt(LocalDateTime.now());
        Notification saved = notifications.save(n);
        audit.record(actor, AuditAction.UPDATE, "NotificationDispatch", saved.getReference(), "SENT", "DELIVERED");
        return saved;
    }

    @Transactional
    public Notification markFailed(Long id, String reason, boolean retryable, String actor) {
        Notification n = notification(id);
        n.setLastAttemptAt(LocalDateTime.now());
        n.setRetryCount(n.getRetryCount() + 1);
        n.setFailureReason(reason);
        n.setStatus(retryable ? NotificationStatus.RETRY_PENDING : NotificationStatus.FAILED);
        Notification saved = notifications.save(n);
        audit.record(actor, AuditAction.UPDATE, "NotificationDispatch", saved.getReference(),
                "PENDING", saved.getStatus().name() + ": " + reason);
        return saved;
    }

    @Transactional
    public FollowUp recordResponse(Long followUpId, FollowUpOutcome outcome,
                                   String note, LocalDate bookedOn, String actor) {
        FollowUp f = get(followUpId);
        f.setOutcome(outcome);
        f.setResponseNote(note);
        f.setRespondedOn(LocalDate.now());
        if (outcome == FollowUpOutcome.BOOKED) {
            f.setBookedOn(bookedOn == null ? LocalDate.now() : bookedOn);
            f.setStatus(FollowUpStatus.NOTIFIED);
        } else if (outcome == FollowUpOutcome.DECLINED) {
            f.setStatus(FollowUpStatus.DEFAULTED);
        }
        FollowUp saved = followUps.save(f);
        audit.record(actor, AuditAction.UPDATE, "PatientFollowUpCase", followUpId.toString(),
                null, outcome.name());
        return saved;
    }

    @Transactional
    public FollowUp markAttended(Long followUpId, String actor) {
        FollowUp f = get(followUpId);
        f.setStatus(FollowUpStatus.ATTENDED);
        FollowUp saved = followUps.save(f);
        audit.record(actor, AuditAction.UPDATE, "PatientFollowUpCase", followUpId.toString(),
                null, "ATTENDED");
        return saved;
    }

    private Notification notification(Long id) {
        return notifications.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No notification with id " + id));
    }

    private Notification saveExcluded(FollowUp f, String reason, String actor) {
        Notification n = build(f, f.getPhone(), NotificationStatus.EXCLUDED);
        n.setSkippedReason(reason);
        Notification saved = notifications.save(n);
        audit.record(actor, AuditAction.CREATE, "NotificationDispatch", saved.getReference(), null, "EXCLUDED: " + reason);
        return saved;
    }

    private Notification build(FollowUp f, String destination, NotificationStatus status) {
        Notification n = new Notification();
        n.setReference(nextReference());
        n.setFollowUpId(f.getId());
        n.setPatientId(f.getPatientId());
        patients.findById(f.getPatientId()).map(Patient::getUserId).ifPresent(n::setRecipientUserId);
        n.setPatientName(f.getPatientName());
        n.setType(f.isHighRisk() ? NotificationType.SPECIAL_CASE_FOLLOWUP : NotificationType.VISIT_REMINDER);
        n.setChannel(NotificationChannel.SMS);
        n.setDestination(destination);
        n.setMessage("Nethcare: " + f.getPatientName() + ", your " + f.getDueForWho().toLowerCase()
                + " is due on " + f.getDueOn() + ". Please contact the clinic to arrange your visit.");
        n.setStatus(status);
        n.setScheduledFor(LocalDateTime.now());
        return n;
    }

    private String normalizeSriLankanPhone(String raw) {
        if (raw == null) return null;
        String value = raw.replaceAll("[\\s()-]", "");
        if (value.matches("07\\d{8}")) return "+94" + value.substring(1);
        if (value.matches("\\+947\\d{8}")) return value;
        return null;
    }

    private String nextReference() {
        long max = notifications.findAll().stream().map(Notification::getReference)
                .filter(r -> r != null && r.matches("NTF-\\d+"))
                .mapToLong(r -> Long.parseLong(r.substring(4))).max().orElse(0L);
        return String.format("NTF-%06d", max + 1);
    }
}
