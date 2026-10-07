package com.nethcare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nethcare.model.*;
import com.nethcare.service.ClinicalAdviceService;
import com.nethcare.service.FollowUpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OPTICIAN", username = "optician_test")
class M4ApiControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean FollowUpService followUpService;
    @MockBean ClinicalAdviceService clinicalAdviceService;

    @Test
    void getDueFollowUpsReturnsList() throws Exception {
        FollowUp f = new FollowUp();
        f.setId(1L);
        f.setPatientName("John Doe");
        f.setDueOn(LocalDate.of(2026, 11, 1));
        f.setCategory(FollowUpCategory.ROUTINE_REVIEW);
        f.setStatus(FollowUpStatus.ACTIVE);

        when(followUpService.weeklyList(any(), any())).thenReturn(List.of(f));

        mvc.perform(get("/api/followups/due")
                        .param("from", "2026-10-01")
                        .param("to", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].patientName").value("John Doe"));
    }

    @Test
    void createFollowUpViaApi() throws Exception {
        M4ApiController.FollowUpRequest req = new M4ApiController.FollowUpRequest(
                10L, 2L, 5L, FollowUpCategory.POST_OPERATIVE,
                LocalDate.of(2026, 12, 1), true, "Review intraocular pressure"
        );

        FollowUp created = new FollowUp();
        created.setId(22L);
        created.setPatientId(10L);
        created.setCategory(FollowUpCategory.POST_OPERATIVE);
        created.setHighRisk(true);

        when(followUpService.createCase(eq(10L), eq(2L), eq(5L), eq(FollowUpCategory.POST_OPERATIVE),
                eq(LocalDate.of(2026, 12, 1)), eq(true), eq("Review intraocular pressure"), anyString()))
                .thenReturn(created);

        mvc.perform(post("/api/followups")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(22));
    }

    @Test
    void postClinicalAdviceViaApi() throws Exception {
        M4ApiController.AdviceRequest req = new M4ApiController.AdviceRequest(
                10L, 2L, 5L, "Rest eyes and return if red",
                "IOP 16 mmHg, Cornea clear", false, null
        );

        ClinicalAdviceLog log = new ClinicalAdviceLog();
        log.setId(88L);
        log.setPatientId(10L);
        log.setRecordHash("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");

        when(clinicalAdviceService.append(eq(10L), eq(2L), eq(5L), anyString(), anyString(), eq(false), isNull(), anyString()))
                .thenReturn(log);

        mvc.perform(post("/api/clinical-advice")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(88))
                .andExpect(jsonPath("$.data.recordHash").value("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"));
    }

    @Test
    void markNotificationDeliveredViaApi() throws Exception {
        Notification n = new Notification();
        n.setId(77L);
        n.setStatus(NotificationStatus.DELIVERED);

        when(followUpService.markDelivered(eq(77L), anyString())).thenReturn(n);

        mvc.perform(post("/api/followups/notifications/77/delivered")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        verify(followUpService).markDelivered(eq(77L), anyString());
    }
}
