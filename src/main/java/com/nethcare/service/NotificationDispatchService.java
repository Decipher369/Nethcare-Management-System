package com.nethcare.service;

import com.nethcare.model.Notification;
import com.nethcare.model.NotificationStatus;
import com.nethcare.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRepository notifications;
    private final FollowUpService followUps;
    private final SmsGateway smsGateway;
    private final EmailGateway emailGateway;
    private final int maxRetries;

    public NotificationDispatchService(NotificationRepository notifications,
                                     FollowUpService followUps,
                                     SmsGateway smsGateway,
                                     EmailGateway emailGateway,
                                     @Value("${nethcare.sms.max-retries:3}") int maxRetries) {
        this.notifications = notifications;
        this.followUps = followUps;
        this.smsGateway = smsGateway;
        this.emailGateway = emailGateway;
        this.maxRetries = maxRetries;
    }

    public record DispatchResult(int queuedCount, int sentCount, int failedCount) {}

    @Transactional
    public DispatchResult dispatchDue(String actor) {
        int queuedCohort = followUps.queueCohort(LocalDate.now(), LocalDate.now().plusDays(2), actor);
        int queuedOrders = followUps.queueNewReadyOrders(actor);
        int totalQueued = queuedCohort + queuedOrders;

        List<Notification> due = notifications
                .findByStatusInAndScheduledForLessThanEqualOrderByScheduledForAsc(
                        List.of(NotificationStatus.PENDING, NotificationStatus.RETRY_PENDING),
                        LocalDateTime.now());

        int sent = 0;
        int failed = 0;

        for (Notification row : due) {
            boolean success = dispatchSingle(row, actor);
            if (success) {
                sent++;
            } else {
                failed++;
            }
        }

        log.info("Dispatched due notifications: queued={}, sent={}, failed={}, provider={}",
                totalQueued, sent, failed, smsGateway.getProviderName());
        return new DispatchResult(totalQueued, sent, failed);
    }

    @Transactional
    public boolean dispatchSingle(Notification row, String actor) {
        if (row.getRetryCount() >= maxRetries) {
            followUps.markFailed(row.getId(), "Exceeded maximum retry limit of " + maxRetries, false, actor);
            return false;
        }

        try {
            String receipt;
            if (row.getChannel() == com.nethcare.model.NotificationChannel.EMAIL) {
                receipt = emailGateway.send(row.getDestination(), row.getMessage());
            } else {
                receipt = smsGateway.send(row.getDestination(), row.getMessage());
            }
            followUps.markSent(row.getId(), receipt, actor);
            return true;
        } catch (Exception failure) {
            boolean retryable = (row.getRetryCount() + 1) < maxRetries;
            String reason = failure.getMessage() != null && !failure.getMessage().isBlank()
                    ? failure.getMessage()
                    : failure.getClass().getSimpleName();
            followUps.markFailed(row.getId(), reason, retryable, actor);
            return false;
        }
    }

    @Transactional
    public boolean dispatchById(Long id, String actor) {
        Notification n = notifications.findById(id).orElse(null);
        if (n == null || !n.isQueued()) {
            return false;
        }
        return dispatchSingle(n, actor);
    }

    public SmsGateway getGateway() {
        return smsGateway;
    }

    public EmailGateway getEmailGateway() {
        return emailGateway;
    }
}
