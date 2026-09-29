package com.nethcare.service;

import com.nethcare.model.Notification;
import com.nethcare.model.NotificationStatus;
import com.nethcare.repository.NotificationRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class NotificationDispatchJob {
    private final NotificationRepository notifications;
    private final FollowUpService followUps;
    private final ObjectProvider<SmsGateway> gateway;

    public NotificationDispatchJob(NotificationRepository notifications, FollowUpService followUps,
                                   ObjectProvider<SmsGateway> gateway) {
        this.notifications = notifications; this.followUps = followUps; this.gateway = gateway;
    }

    @Scheduled(cron = "${nethcare.sms.dispatch-cron:0 0 8 * * *}", zone = "Asia/Colombo")
    public void dispatchDue() {
        followUps.queueCohort(java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(2), "system-sms");
        followUps.queueNewReadyOrders("system-sms");
        SmsGateway sms = gateway.getIfAvailable();
        if (sms == null) return;
        List<Notification> due = notifications
                .findByStatusInAndScheduledForLessThanEqualOrderByScheduledForAsc(
                        List.of(NotificationStatus.PENDING, NotificationStatus.RETRY_PENDING), LocalDateTime.now());
        for (Notification row : due) {
            try {
                String receipt = sms.send(row.getDestination(), row.getMessage());
                followUps.markSent(row.getId(), receipt, "system-sms");
            } catch (RuntimeException failure) {
                followUps.markFailed(row.getId(), safeMessage(failure), true, "system-sms");
            }
        }
    }

    private String safeMessage(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }
}
