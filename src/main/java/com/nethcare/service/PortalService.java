package com.nethcare.service;

import com.nethcare.dto.PatientDto;
import com.nethcare.dto.PortalData;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.FollowUp;
import com.nethcare.model.Patient;
import com.nethcare.repository.FollowUpRepository;
import com.nethcare.repository.OrderRepository;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.PrescriptionRepository;
import com.nethcare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PortalService {
    private final UserRepository users;
    private final PatientRepository patients;
    private final PrescriptionRepository prescriptions;
    private final OrderRepository orders;
    private final FollowUpRepository followUps;

    public PortalService(UserRepository users, PatientRepository patients,
                         PrescriptionRepository prescriptions, OrderRepository orders,
                         FollowUpRepository followUps) {
        this.users = users;
        this.patients = patients;
        this.prescriptions = prescriptions;
        this.orders = orders;
        this.followUps = followUps;
    }

    @Transactional(readOnly = true)
    public PortalData forUser(String username) {
        var user = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        Patient patient = patients.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("This account is not linked to a patient record. Ask reception to link it."));

        LocalDate today = LocalDate.now();
        List<FollowUp> consultations = followUps.findByPatientIdOrderByDueOnAsc(patient.getId()).stream()
                .filter(f -> f.getDueOn() != null && !f.getDueOn().isBefore(today) && f.isOpen())
                .limit(5)
                .toList();

        return new PortalData(
                PatientDto.of(patient),
                prescriptions.findByPatientIdOrderByIssuedOnDesc(patient.getId()),
                orders.findByPatientIdOrderByOrderedOnDescIdDesc(patient.getId()),
                consultations
        );
    }
}
