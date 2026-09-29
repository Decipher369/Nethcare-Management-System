package com.nethcare.service;

import com.nethcare.dto.PatientForm;
import com.nethcare.exception.BusinessException;
import com.nethcare.model.NumberSequence;
import com.nethcare.model.User;
import com.nethcare.repository.NumberSequenceRepository;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock PatientRepository patients;
    @Mock UserRepository users;
    @Mock NumberSequenceRepository sequences;
    @Mock PasswordEncoder encoder;
    @Mock AuditService audit;
    PatientService service;

    @BeforeEach void setUp() { service = new PatientService(patients, users, sequences, encoder, audit); }

    @Test
    void createsLinkedAccountWithOneTimeReadableCredentialAndForcedPasswordChange() {
        PatientForm form = adult(); form.setCreateLogin(true);
        NumberSequence sequence = new NumberSequence(); sequence.setSequenceName("PATIENT"); sequence.setNextValue(5);
        when(sequences.lockByName("PATIENT")).thenReturn(Optional.of(sequence));
        when(users.findByUsername(anyString())).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("bcrypt");
        when(users.save(any(User.class))).thenAnswer(call -> { User u = call.getArgument(0); u.setId(9L); return u; });
        when(patients.save(any())).thenAnswer(call -> { var p = call.getArgument(0, com.nethcare.model.Patient.class); p.setId(7L); return p; });

        var result = service.register(form, "optician");

        assertEquals("P-0005", result.patient().getPatientNo());
        assertEquals(9L, result.patient().getUserId());
        assertTrue(result.credential().temporaryPassword().startsWith("Nimal@19900102-"));
        verify(users).save(argThat(user -> user.isMustChangePassword() && "bcrypt".equals(user.getPasswordHash())));
        verify(audit).record(eq("optician"), any(), eq("Patient"), eq("7"), isNull(), anyString(), anyString());
    }

    @Test
    void rejectsMinorWithoutGuardianInsteadOfCreatingAnAmbiguousRecord() {
        PatientForm form = new PatientForm();
        form.setFullName("Child Patient"); form.setDob(LocalDate.now().minusYears(10).toString());
        assertThrows(BusinessException.class, () -> service.register(form, "optician"));
        verifyNoInteractions(sequences, encoder, audit);
    }

    @Test
    void rejectsDuplicateAdultNic() {
        PatientForm form = adult();
        when(patients.existsByNicIgnoreCaseAndIdNot("901234567V", -1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.register(form, "optician"));
        verify(patients, never()).save(any());
    }

    private PatientForm adult() {
        PatientForm form = new PatientForm();
        form.setFullName("Nimal Perera"); form.setDob("1990-01-02");
        form.setNic("901234567v"); form.setPhone("0771234567");
        form.setConsentGiven(true);
        return form;
    }
}
