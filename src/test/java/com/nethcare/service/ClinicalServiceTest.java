package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.model.ClinicalSymptom;
import com.nethcare.model.Examination;
import com.nethcare.model.MedicalHistory;
import com.nethcare.model.Referral;
import com.nethcare.repository.ClinicalSymptomRepository;
import com.nethcare.repository.ExaminationRepository;
import com.nethcare.repository.MedicalHistoryRepository;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.PrescriptionRepository;
import com.nethcare.repository.ReferralRepository;
import com.nethcare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalServiceTest {

    @Mock ExaminationRepository examinations;
    @Mock PrescriptionRepository prescriptions;
    @Mock ReferralRepository referrals;
    @Mock PatientRepository patients;
    @Mock ClinicalSymptomRepository symptoms;
    @Mock MedicalHistoryRepository histories;
    @Mock UserRepository users;

    ClinicalService service;

    @BeforeEach
    void setUp() {
        service = new ClinicalService(examinations, prescriptions, referrals,
                patients, symptoms, histories, users);
    }

    @Test
    void recordsExamSymptomsAndVersionedHistoryTogether() {
        Examination exam = examWithFindings();
        ClinicalSymptom symptom = new ClinicalSymptom();
        symptom.setBlurVision(true);
        MedicalHistory history = new MedicalHistory();
        history.setDiabetic(true);

        when(patients.existsById(7L)).thenReturn(true);
        when(histories.countByPatientId(7L)).thenReturn(2L);
        when(examinations.save(exam)).thenAnswer(invocation -> {
            exam.setId(42L);
            return exam;
        });

        service.recordExamination(exam, symptom, history, "optician");

        assertEquals(42L, symptom.getExaminationId());
        assertEquals(42L, history.getExaminationId());
        assertEquals(3, history.getVersionNumber());
        assertEquals("optician", history.getRecordedBy());
        verify(symptoms).save(symptom);
        verify(histories).save(history);
    }

    @Test
    void rejectsAxisOutsideClinicalRangeBeforeSaving() {
        Examination exam = examWithFindings();
        exam.setOdAxis("181");
        when(patients.existsById(7L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.recordExamination(
                exam, new ClinicalSymptom(), new MedicalHistory(), "optician"));

        verify(examinations, never()).save(any());
    }

    @Test
    void preventsIssuingTwoPrescriptionsFromOneExam() {
        Examination exam = examWithFindings();
        exam.setId(42L);
        when(examinations.findById(42L)).thenReturn(Optional.of(exam));
        when(prescriptions.existsByExaminationId(42L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.issueFrom(42L, "optician"));
        verify(prescriptions, never()).save(any());
    }

    @Test
    void onlyAssignedSurgeonCanCompleteConsultation() {
        Referral referral = new Referral();
        referral.setSurgeonName("surgeon-a");
        referral.setStatus("PENDING");
        when(referrals.findById(9L)).thenReturn(Optional.of(referral));

        assertThrows(BusinessException.class, () -> service.recordConsultation(
                9L, "surgeon-b", false, null, "Cataract", "Surgery", null, null));
        verify(referrals, never()).save(any());
    }

    @Test
    void completesConsultationWithoutAgeBasedAccessCutoff() {
        Referral referral = new Referral();
        referral.setSurgeonName("surgeon-a");
        referral.setStatus("PENDING");
        referral.setReferredOn(LocalDate.now().minusYears(2));
        when(referrals.findById(9L)).thenReturn(Optional.of(referral));
        when(referrals.save(referral)).thenReturn(referral);

        service.recordConsultation(9L, "surgeon-a", false,
                "OCT", "Retinal change", "Medication", "Review in clinic", "Stable");

        assertEquals("COMPLETED", referral.getStatus());
        assertEquals("Retinal change", referral.getDiagnosis());
        verify(referrals).save(referral);
    }

    private Examination examWithFindings() {
        Examination exam = new Examination();
        exam.setPatientId(7L);
        exam.setExamDate(LocalDate.now());
        exam.setFindings("Routine refraction");
        return exam;
    }
}
