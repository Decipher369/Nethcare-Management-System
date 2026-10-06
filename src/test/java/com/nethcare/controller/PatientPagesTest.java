package com.nethcare.controller;

import com.nethcare.model.Patient;
import com.nethcare.service.ClinicalService;
import com.nethcare.service.PatientService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OPTICIAN")
class PatientPagesTest {
    @Autowired MockMvc mvc;
    @MockBean PatientService patients;
    @MockBean ClinicalService clinical;

    @Test
    void redesignedSummaryPreservesSmsFormAndErrorFeedback() throws Exception {
        Patient patient = new Patient();
        patient.setId(54L);
        patient.setPatientNo("P-0054");
        patient.setFullName("Test Patient");
        patient.setDob(LocalDate.of(1990, 1, 1));
        patient.setRegisteredOn(LocalDate.of(2026, 1, 1));
        patient.setPhone("0771234567");
        patient.setIsActive(true);
        when(patients.get(54L)).thenReturn(patient);
        when(clinical.historyFor(54L)).thenReturn(List.of());
        when(clinical.prescriptionsFor(54L)).thenReturn(List.of());

        mvc.perform(get("/patients/54").flashAttr("error", "SMS delivery failed"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/detail"))
                .andExpect(content().string(containsString("patient-summary-page")))
                .andExpect(content().string(containsString("Test Patient")))
                .andExpect(content().string(containsString("action=\"/patients/54/send-sms\"")))
                .andExpect(content().string(containsString("name=\"message\"")))
                .andExpect(content().string(containsString("SMS delivery failed")))
                .andExpect(content().string(containsString("href=\"/notifications\"")));
    }

    @Test
    void registrationFormStillRendersAfterMainMerge() throws Exception {
        mvc.perform(get("/patients/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/form"))
                .andExpect(content().string(containsString("patient-register-page")))
                .andExpect(content().string(containsString("action=\"/patients\"")));
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void redesignedUserAdministrationPreservesSharedRailAndCreateForm() throws Exception {
        mvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(containsString("user-admin-page")))
                .andExpect(content().string(containsString("Administration navigation")))
                .andExpect(content().string(containsString("/css/admin.css")))
                .andExpect(content().string(containsString("action=\"/admin/users\"")));
    }
}
