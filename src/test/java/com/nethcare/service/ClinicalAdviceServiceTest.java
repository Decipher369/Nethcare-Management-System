package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.model.ClinicalAdviceLog;
import com.nethcare.repository.ClinicalAdviceLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicalAdviceServiceTest {
    @Mock ClinicalAdviceLogRepository logs;
    @Mock AuditService audit;

    @Test
    void highRiskAdviceRequiresCautionText() {
        ClinicalAdviceService service = new ClinicalAdviceService(logs, audit);
        assertThrows(BusinessException.class, () -> service.append(
                1L, 2L, 3L, "Return immediately if pain worsens", "IOP=28",
                true, " ", "optician"));
        verifyNoInteractions(logs, audit);
    }

    @Test
    void appendsHashSealedAdviceAndAuditInOneWorkflow() {
        when(logs.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(logs.save(any())).thenAnswer(invocation -> {
            ClinicalAdviceLog row = invocation.getArgument(0);
            row.setId(42L);
            return row;
        });
        ClinicalAdviceService service = new ClinicalAdviceService(logs, audit);

        ClinicalAdviceLog saved = service.append(1L, 2L, 3L,
                "Attend tomorrow", "Corneal staining", true,
                "Seek urgent care for increasing pain", "optician");

        assertEquals(64, saved.getRecordHash().length());
        assertNull(saved.getPreviousHash());
        assertNotNull(saved.getRecordedAtUtc());
        verify(audit).record(eq("optician"), any(), eq("ClinicalAdviceLog"),
                eq("42"), isNull(), eq(saved.getRecordHash()), anyString());
    }
}
