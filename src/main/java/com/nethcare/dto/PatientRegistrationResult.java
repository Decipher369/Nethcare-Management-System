package com.nethcare.dto;

import com.nethcare.model.Patient;

public record PatientRegistrationResult(Patient patient, TemporaryCredential credential) { }
