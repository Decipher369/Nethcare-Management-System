package com.nethcare.controller;

import com.nethcare.model.*;
import com.nethcare.repository.*;
import com.nethcare.service.FollowUpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OPTICIAN", username = "optician_test")
class FollowUpPagesTest {

    @Autowired MockMvc mvc;
    @MockBean FollowUpService followUpService;
    @MockBean PatientRepository patients;
    @MockBean ExaminationRepository examinations;
    @MockBean UserRepository users;

    @Test
    void followupsPageRendersWithScheduleFormAndResponseButtons() throws Exception {
        FollowUp case1 = new FollowUp();
        case1.setId(101L);
        case1.setPatientId(55L);
        case1.setPatientName("Sita Ram");
        case1.setDueOn(LocalDate.now().plusMonths(6));
        case1.setCategory(FollowUpCategory.ROUTINE_REVIEW);
        case1.setStatus(FollowUpStatus.ACTIVE);
        case1.setPhone("0771234567");

        when(followUpService.weeklyList(any(), any())).thenReturn(List.of(case1));
        when(patients.findAll()).thenReturn(List.of());
        when(users.findByRoleOrderByFullNameAsc(Role.OPTICIAN)).thenReturn(List.of());

        mvc.perform(get("/followups"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/followups"))
                .andExpect(content().string(containsString("Sita Ram")))
                .andExpect(content().string(containsString("Schedule New Patient Follow-up Review")))
                .andExpect(content().string(containsString("action=\"/followups/101/booked\"")))
                .andExpect(content().string(containsString("action=\"/followups/101/declined\"")))
                .andExpect(content().string(containsString("action=\"/followups/101/attended\"")));
    }

    @Test
    void markBookedSubmitsAndRedirects() throws Exception {
        mvc.perform(post("/followups/101/booked")
                        .with(csrf())
                        .param("bookedOn", "2026-10-15")
                        .param("note", "Confirmed appointment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/followups"))
                .andExpect(flash().attributeExists("caseMessage"));

        verify(followUpService).recordResponse(eq(101L), eq(FollowUpOutcome.BOOKED), eq("Confirmed appointment"), eq(LocalDate.of(2026, 10, 15)), anyString());
    }

    @Test
    void markDeclinedSubmitsAndRedirects() throws Exception {
        mvc.perform(post("/followups/101/declined")
                        .with(csrf())
                        .param("note", "Patient moved overseas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/followups"))
                .andExpect(flash().attributeExists("caseMessage"));

        verify(followUpService).recordResponse(eq(101L), eq(FollowUpOutcome.DECLINED), eq("Patient moved overseas"), isNull(), anyString());
    }

    @Test
    void markAttendedSubmitsAndRedirects() throws Exception {
        mvc.perform(post("/followups/101/attended")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/followups"))
                .andExpect(flash().attributeExists("caseMessage"));

        verify(followUpService).markAttended(eq(101L), anyString());
    }

    @Test
    void scheduleFollowUpSubmitsAndRedirects() throws Exception {
        FollowUp created = new FollowUp();
        created.setId(201L);
        created.setPatientName("Sunil Perera");
        created.setDueOn(LocalDate.of(2027, 4, 10));

        Examination exam = new Examination();
        exam.setId(301L);
        exam.setPatientId(55L);

        when(examinations.findByPatientIdOrderByExamDateDesc(55L)).thenReturn(List.of(exam));
        when(followUpService.createCase(eq(55L), anyLong(), eq(301L), eq(FollowUpCategory.ROUTINE_REVIEW),
                eq(LocalDate.of(2027, 4, 10)), eq(false), any(), anyString())).thenReturn(created);

        User optician = new User();
        optician.setId(5L);
        optician.setRole(Role.OPTICIAN);
        when(users.findByUsername("optician_test")).thenReturn(Optional.of(optician));

        mvc.perform(post("/followups/schedule")
                        .with(csrf())
                        .param("patientId", "55")
                        .param("category", "ROUTINE_REVIEW")
                        .param("targetReviewDate", "2027-04-10")
                        .param("highRisk", "false")
                        .param("clinicalNotes", "6-month routine review"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/followups"))
                .andExpect(flash().attributeExists("caseMessage"));
    }

    @Test
    void scheduleFollowUpFromExaminationSubmitsAndRedirects() throws Exception {
        FollowUp created = new FollowUp();
        created.setId(202L);
        created.setPatientName("Sunil Perera");
        created.setDueOn(LocalDate.of(2027, 4, 10));

        Examination exam = new Examination();
        exam.setId(301L);
        exam.setPatientId(55L);
        exam.setExaminedBy("optician_test");

        when(examinations.findById(301L)).thenReturn(Optional.of(exam));
        when(followUpService.createCase(eq(55L), anyLong(), eq(301L), eq(FollowUpCategory.ROUTINE_REVIEW),
                eq(LocalDate.of(2027, 4, 10)), eq(false), any(), anyString())).thenReturn(created);

        User optician = new User();
        optician.setId(5L);
        optician.setRole(Role.OPTICIAN);
        when(users.findByUsername("optician_test")).thenReturn(Optional.of(optician));

        mvc.perform(post("/examinations/301/followup")
                        .with(csrf())
                        .param("category", "ROUTINE_REVIEW")
                        .param("targetReviewDate", "2027-04-10")
                        .param("highRisk", "false")
                        .param("clinicalNotes", "Review after 6 months"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/examinations/301"))
                .andExpect(flash().attributeExists("msg"));
    }

    @Test
    void followupsPageSupportsPresetPillsAndHighRiskFilter() throws Exception {
        FollowUp case1 = new FollowUp();
        case1.setId(101L);
        case1.setPatientId(55L);
        case1.setPatientName("Sita Ram");
        case1.setDueOn(LocalDate.now());
        case1.setCategory(FollowUpCategory.ROUTINE_REVIEW);
        case1.setStatus(FollowUpStatus.ACTIVE);
        case1.setHighRisk(true);
        case1.setPhone("0771234567");

        when(followUpService.weeklyList(eq(LocalDate.now()), eq(LocalDate.now()))).thenReturn(List.of(case1));
        when(patients.findAll()).thenReturn(List.of());
        when(users.findByRoleOrderByFullNameAsc(Role.OPTICIAN)).thenReturn(List.of());

        mvc.perform(get("/followups")
                        .param("preset", "today")
                        .param("highRisk", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/followups"))
                .andExpect(content().string(containsString("filter-pill")))
                .andExpect(content().string(containsString("active")))
                .andExpect(content().string(containsString("Today")))
                .andExpect(content().string(containsString("Sita Ram")));
    }

    @Test
    void testFollowupsWithEmptyCategoryParam() throws Exception {
        mvc.perform(get("/followups?preset=today&highRisk=true&category="))
                .andExpect(status().isOk());
    }

    @Test
    void testFollowupsWithOverduePreset() throws Exception {
        mvc.perform(get("/followups?preset=overdue"))
                .andExpect(status().isOk());
    }
}
