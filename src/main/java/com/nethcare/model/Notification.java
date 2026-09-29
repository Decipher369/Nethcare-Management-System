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
 * The row keeps the exact message, destination, gateway receipt, attempts and
 * timestamps. A message becomes SENT only when a real gateway receipt is supplied.
 */
@Entity
@Table(name = "notification_dispatches")
public class Notification extends BaseEntity {

    @Column(name = "reference", nullable = false, length = 20)
    private String reference;

    @Column(name = "follow_up_id")
    private Long followUpId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType type = NotificationType.VISIT_REMINDER;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", nullable = false, length = 20)
    private NotificationRecipientRole recipientRole = NotificationRecipientRole.PATIENT;

    @Column(name = "patient_name", length = 120)
    private String patientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, columnDefinition = "VARCHAR(10)")
    private NotificationChannel channel;

    @Column(name = "destination", length = 120)
    private String destination;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "gateway_receipt_id", length = 100)
    private String gatewayReceiptId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    /** Set when the row was skipped because the patient opted out. */
    @Column(name = "skipped_reason", length = 200)
    private String skippedReason;

    public boolean isQueued() {
        return status == NotificationStatus.PENDING || status == NotificationStatus.RETRY_PENDING;
    }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Long getFollowUpId() { return followUpId; }
    public void setFollowUpId(Long followUpId) { this.followUpId = followUpId; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Long recipientUserId) { this.recipientUserId = recipientUserId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public NotificationRecipientRole getRecipientRole() { return recipientRole; }
    public void setRecipientRole(NotificationRecipientRole recipientRole) { this.recipientRole = recipientRole; }

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
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getGatewayReceiptId() { return gatewayReceiptId; }
    public void setGatewayReceiptId(String gatewayReceiptId) { this.gatewayReceiptId = gatewayReceiptId; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getSkippedReason() { return skippedReason; }
    public void setSkippedReason(String skippedReason) { this.skippedReason = skippedReason; }
}
