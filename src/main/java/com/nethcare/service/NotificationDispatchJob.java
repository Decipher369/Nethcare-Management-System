package com.nethcare.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationDispatchJob {

    private final NotificationDispatchService dispatchService;

    public NotificationDispatchJob(NotificationDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Scheduled(cron = "${nethcare.sms.dispatch-cron:0 0 8 * * *}", zone = "Asia/Colombo")
    public void dispatchDue() {
        dispatchService.dispatchDue("system-sms");
    }
}
