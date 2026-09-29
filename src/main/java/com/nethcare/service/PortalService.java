package com.nethcare.service;

import com.nethcare.dto.PatientDto;
import com.nethcare.dto.PortalData;
import com.nethcare.exception.ResourceNotFoundException;
import com.nethcare.model.Patient;
import com.nethcare.repository.OrderRepository;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.PrescriptionRepository;
import com.nethcare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalService {
    private final UserRepository users;
    private final PatientRepository patients;
    private final PrescriptionRepository prescriptions;
    private final OrderRepository orders;

    public PortalService(UserRepository users, PatientRepository patients,
                         PrescriptionRepository prescriptions, OrderRepository orders) {
        this.users = users; this.patients = patients;
        this.prescriptions = prescriptions; this.orders = orders;
    }

    @Transactional(readOnly = true)
    public PortalData forUser(String username) {
        var user = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        Patient patient = patients.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("This account is not linked to a patient record. Ask reception to link it."));
        return new PortalData(PatientDto.of(patient),
                prescriptions.findByPatientIdOrderByIssuedOnDesc(patient.getId()),
                orders.findByPatientIdOrderByOrderedOnDescIdDesc(patient.getId()));
    }
}
