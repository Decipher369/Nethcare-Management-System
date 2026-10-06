package com.nethcare.dto;

import com.nethcare.model.FollowUp;
import com.nethcare.model.Order;
import com.nethcare.model.Prescription;
import java.util.List;

public record PortalData(
    PatientDto patient,
    List<Prescription> prescriptions,
    List<Order> orders,
    List<FollowUp> consultations
) { }
