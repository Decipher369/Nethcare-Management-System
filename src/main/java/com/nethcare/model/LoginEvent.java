package com.nethcare.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_events", indexes = {
        @Index(name = "idx_login_event_time", columnList = "occurred_at"),
        @Index(name = "idx_login_event_user", columnList = "user_id,occurred_at")
})
public class LoginEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(name = "username", nullable = false, length = 50) private String username;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
    @Column(name = "successful", nullable = false) private boolean successful;
    @Column(name = "failure_reason", length = 100) private String failureReason;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "user_agent", length = 300) private String userAgent;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean successful) { this.successful = successful; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
