package com.nethcare.service;

import com.nethcare.model.AuditAction;
import com.nethcare.model.AuditLog;
import com.nethcare.model.User;
import com.nethcare.repository.AuditLogRepository;
import com.nethcare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void recordFirstEntryGeneratesValidSha256Hash() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User() {{ setId(1L); setUsername("admin"); }}));
        when(auditLogRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditService service = new AuditService(auditLogRepository, userRepository);
        AuditLog entry = service.record("admin", AuditAction.CREATE, "Patient", "10", null, "New Patient Record", "Registration note");

        assertNotNull(entry);
        assertNull(entry.getPreviousHash());
        assertNotNull(entry.getRecordHash());
        assertEquals(64, entry.getRecordHash().length());
        assertEquals("admin", entry.getActor());
        assertEquals(1L, entry.getActorId());
        assertEquals(AuditAction.CREATE, entry.getAction());
        assertEquals("Patient", entry.getEntityName());
        assertEquals("10", entry.getEntityId());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void recordSecondEntryChainsPreviousHash() {
        AuditLog first = new AuditLog();
        first.setId(1L);
        first.setRecordHash("a".repeat(64));

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(auditLogRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(first));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditService service = new AuditService(auditLogRepository, userRepository);
        AuditLog second = service.record("optician", AuditAction.UPDATE, "Patient", "10", "Old Name", "New Name");

        assertNotNull(second);
        assertEquals(first.getRecordHash(), second.getPreviousHash());
        assertNotNull(second.getRecordHash());
        assertEquals(64, second.getRecordHash().length());
        assertNotEquals(first.getRecordHash(), second.getRecordHash());
    }

    @Test
    void verifyChainReturnsTrueForValidChain() {
        AuditService service = new AuditService(auditLogRepository, userRepository);

        LocalDateTime now = LocalDateTime.now(Clock.systemUTC()).truncatedTo(ChronoUnit.MICROS);

        AuditLog log1 = new AuditLog();
        log1.setId(1L);
        log1.setOccurredAt(now);
        log1.setActor("admin");
        log1.setActorId(1L);
        log1.setAction(AuditAction.CREATE);
        log1.setEntityName("Patient");
        log1.setEntityId("10");
        log1.setOldValue(null);
        log1.setNewValue("Created");
        log1.setNote(null);
        log1.setPreviousHash(null);

        // We can create log1 via record with mocks
        when(auditLogRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User() {{ setId(1L); }}));

        AuditLog created1 = service.record("admin", AuditAction.CREATE, "Patient", "10", null, "Created", null);
        created1.setId(1L);

        when(auditLogRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(created1));
        AuditLog created2 = service.record("admin", AuditAction.UPDATE, "Patient", "10", "Created", "Updated", "Note");
        created2.setId(2L);

        when(auditLogRepository.findAll()).thenReturn(List.of(created1, created2));

        assertTrue(service.verifyChain());
    }

    @Test
    void verifyChainReturnsFalseWhenTampered() {
        AuditService service = new AuditService(auditLogRepository, userRepository);

        when(auditLogRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLog created1 = service.record("admin", AuditAction.CREATE, "Patient", "10", null, "Created", null);
        created1.setId(1L);

        when(auditLogRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(created1));
        AuditLog created2 = service.record("admin", AuditAction.UPDATE, "Patient", "10", "Created", "Updated", "Note");
        created2.setId(2L);

        // Tamper with record 1's new value
        created1.setNewValue("Tampered Value");

        when(auditLogRepository.findAll()).thenReturn(List.of(created1, created2));

        assertFalse(service.verifyChain());
    }
}
