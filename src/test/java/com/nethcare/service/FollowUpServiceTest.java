package com.nethcare.service;

import com.nethcare.model.*;
import com.nethcare.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    @Mock FollowUpRepository followUps;
    @Mock NotificationRepository notifications;
    @Mock PatientRepository patients;
    @Mock UserRepository users;
    @Mock ExaminationRepository examinations;
    @Mock OrderRepository orders;
    @Mock AuditService audit;

    private FollowUpService followUpService;

    @BeforeEach
    void setUp() {
        followUpService = new FollowUpService(
                followUps, notifications, patients, users, examinations, orders, audit
        );
    }

    @Test
    void notifyRoutesToSmsWhenPhoneValid() {
        FollowUp f = new FollowUp();
        f.setId(1L);
        f.setPatientId(10L);
        f.setPatientName("Test Patient");
        f.setPhone("0771234567");
        f.setEmail("patient@test.com");
        f.setCategory(FollowUpCategory.ROUTINE_REVIEW);
        f.setDueOn(LocalDate.now().plusMonths(6));

        when(followUps.findById(1L)).thenReturn(Optional.of(f));
        when(patients.findById(10L)).thenReturn(Optional.of(new Patient()));
        when(notifications.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = followUpService.notify(1L, "optician");

        assertNotNull(result);
        assertEquals(NotificationChannel.SMS, result.getChannel());
        assertEquals("+94771234567", result.getDestination());
        assertEquals(NotificationStatus.PENDING, result.getStatus());
    }

    @Test
    void notifyFallbacksToEmailWhenPhoneMissingOrInvalid() {
        FollowUp f = new FollowUp();
        f.setId(2L);
        f.setPatientId(10L);
        f.setPatientName("Test Patient");
        f.setPhone("invalid-phone");
        f.setEmail("patient@test.com");
        f.setCategory(FollowUpCategory.ROUTINE_REVIEW);
        f.setDueOn(LocalDate.now().plusMonths(6));

        when(followUps.findById(2L)).thenReturn(Optional.of(f));
        when(patients.findById(10L)).thenReturn(Optional.of(new Patient()));
        when(notifications.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = followUpService.notify(2L, "optician");

        assertNotNull(result);
        assertEquals(NotificationChannel.EMAIL, result.getChannel());
        assertEquals("patient@test.com", result.getDestination());
        assertEquals(NotificationStatus.PENDING, result.getStatus());
    }

    @Test
    void sendPatientEmailQueuesEmailWhenValid() {
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setFullName("Patient With Email");
        patient.setEmail("hello@clinic.lk");

        when(patients.findById(10L)).thenReturn(Optional.of(patient));
        when(notifications.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = followUpService.sendPatientEmail(10L, "Custom appointment note", "optician");

        assertNotNull(result);
        assertEquals(NotificationChannel.EMAIL, result.getChannel());
        assertEquals("hello@clinic.lk", result.getDestination());
        assertEquals(NotificationStatus.PENDING, result.getStatus());
        assertEquals("Custom appointment note", result.getMessage());
    }

    @Test
    void sendDirectEmailSuccess() {
        when(notifications.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = followUpService.sendDirectEmail("direct@example.com", "Ad-hoc User", "Direct hello", "admin");

        assertNotNull(result);
        assertEquals(NotificationChannel.EMAIL, result.getChannel());
        assertEquals("direct@example.com", result.getDestination());
        assertEquals(NotificationStatus.PENDING, result.getStatus());
    }

    @Test
    void sendPatientNotificationDispatchesBothWhenBothPresent() {
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setFullName("Patient Both");
        patient.setPhone("0771234567");
        patient.setEmail("both@example.com");

        when(patients.findById(10L)).thenReturn(Optional.of(patient));
        when(notifications.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        java.util.List<Notification> list = followUpService.sendPatientNotification(10L, "Appointment text", "AUTO", "optician");

        assertEquals(2, list.size());
        assertEquals(NotificationChannel.SMS, list.get(0).getChannel());
        assertEquals("+94771234567", list.get(0).getDestination());
        assertEquals(NotificationChannel.EMAIL, list.get(1).getChannel());
        assertEquals("both@example.com", list.get(1).getDestination());
    }

    @Test
    void sendPatientNotificationDispatchesEmailWhenPhoneMissing() {
        Patient patient = new Patient();
        patient.setId(11L);
        patient.setFullName("Patient Email Only");
        patient.setPhone(null);
        patient.setEmail("emailonly@example.com");

        when(patients.findById(11L)).thenReturn(Optional.of(patient));
        when(notifications.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        java.util.List<Notification> list = followUpService.sendPatientNotification(11L, "Appointment text", "AUTO", "optician");

        assertEquals(1, list.size());
        assertEquals(NotificationChannel.EMAIL, list.get(0).getChannel());
        assertEquals("emailonly@example.com", list.get(0).getDestination());
    }

    @Test
    void sendPatientNotificationThrowsWhenNeitherAvailable() {
        Patient patient = new Patient();
        patient.setId(12L);
        patient.setFullName("Patient No Contact");
        patient.setPhone(null);
        patient.setEmail(null);

        when(patients.findById(12L)).thenReturn(Optional.of(patient));

        assertThrows(com.nethcare.exception.BusinessException.class, () ->
                followUpService.sendPatientNotification(12L, "Appointment text", "AUTO", "optician")
        );
    }
}
