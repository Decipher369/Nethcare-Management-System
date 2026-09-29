package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One reminder waiting to go out.
 *
 * SMS first, email as the fallback, which is the order the client asked for
 * because a text costs almost nothing and a wrong email is just ignored. The
 * chosen channel is recorded on the row so the queue can show why a patient
 * was emailed instead of texted.
 *
 * Nothing is sent from here. The row is the queue, and a scheduled job (or a
 * staff member pressing the button) marks it sent — the shop has no SMS
 * gateway configured, so claiming a message left the building would be a lie.
 */
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(name = "reference", nullable = false, length = 20)
    private String reference;

    @Column(name = "follow_up_id")
    private Long followUpId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "patient_name", length = 120)
    private String patientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, columnDefinition = "VARCHAR(10)")
    private NotificationChannel channel;

    @Column(name = "destination", length = 120)
    private String destination;

    @Column(name = "message", length = 300)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
    private NotificationStatus status = NotificationStatus.QUEUED;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    /** Set when the row was skipped because the patient opted out. */
    @Column(name = "skipped_reason", length = 200)
    private String skippedReason;

    public boolean isQueued() {
        return status == NotificationStatus.QUEUED;
    }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Long getFollowUpId() { return followUpId; }
    public void setFollowUpId(Long followUpId) { this.followUpId = followUpId; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public LocalDateTime getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(LocalDateTime scheduledFor) { this.scheduledFor = scheduledFor; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getSkippedReason() { return skippedReason; }
    public void setSkippedReason(String skippedReason) { this.skippedReason = skippedReason; }
}
