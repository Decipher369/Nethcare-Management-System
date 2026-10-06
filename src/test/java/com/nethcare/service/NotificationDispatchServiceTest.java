package com.nethcare.service;

import com.nethcare.model.Notification;
import com.nethcare.model.NotificationStatus;
import com.nethcare.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock NotificationRepository notifications;
    @Mock FollowUpService followUps;
    @Mock SmsGateway smsGateway;

    private NotificationDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new NotificationDispatchService(notifications, followUps, smsGateway, 3);
    }

    @Test
    void dispatchSingleSuccess() {
        Notification n = new Notification();
        n.setId(10L);
        n.setDestination("+94771234567");
        n.setMessage("Test SMS");
        n.setStatus(NotificationStatus.PENDING);
        n.setRetryCount(0);

        when(smsGateway.send("+94771234567", "Test SMS")).thenReturn("SIM-RECEIPT-123");

        boolean result = dispatchService.dispatchSingle(n, "tester");

        assertTrue(result);
        verify(followUps).markSent(10L, "SIM-RECEIPT-123", "tester");
    }

    @Test
    void dispatchSingleFailureRecordsRetryPending() {
        Notification n = new Notification();
        n.setId(10L);
        n.setDestination("+94771234567");
        n.setMessage("Test SMS");
        n.setStatus(NotificationStatus.PENDING);
        n.setRetryCount(0);

        when(smsGateway.send("+94771234567", "Test SMS")).thenThrow(new RuntimeException("Connection timeout"));

        boolean result = dispatchService.dispatchSingle(n, "tester");

        assertFalse(result);
        verify(followUps).markFailed(10L, "Connection timeout", true, "tester");
    }

    @Test
    void dispatchSingleExceedingMaxRetriesMarksFailedPermanently() {
        Notification n = new Notification();
        n.setId(10L);
        n.setDestination("+94771234567");
        n.setMessage("Test SMS");
        n.setStatus(NotificationStatus.RETRY_PENDING);
        n.setRetryCount(3);

        boolean result = dispatchService.dispatchSingle(n, "tester");

        assertFalse(result);
        verify(smsGateway, never()).send(anyString(), anyString());
        verify(followUps).markFailed(eq(10L), contains("Exceeded maximum retry limit"), eq(false), eq("tester"));
    }

    @Test
    void dispatchByIdSendsWhenQueued() {
        Notification n = new Notification();
        n.setId(42L);
        n.setDestination("+94771234567");
        n.setMessage("Test SMS");
        n.setStatus(NotificationStatus.PENDING);

        when(notifications.findById(42L)).thenReturn(Optional.of(n));
        when(smsGateway.send("+94771234567", "Test SMS")).thenReturn("SIM-RCP-42");

        boolean result = dispatchService.dispatchById(42L, "staff");

        assertTrue(result);
        verify(followUps).markSent(42L, "SIM-RCP-42", "staff");
    }
}
