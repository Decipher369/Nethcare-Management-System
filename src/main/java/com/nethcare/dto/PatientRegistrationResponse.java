package com.nethcare.dto;

public record PatientRegistrationResponse(
        PatientDto patient,
        String username,
        String temporaryPassword
) {
    public static PatientRegistrationResponse of(PatientRegistrationResult result) {
        TemporaryCredential credential = result.credential();
        return new PatientRegistrationResponse(PatientDto.of(result.patient()),
                credential == null ? null : credential.username(),
                credential == null ? null : credential.temporaryPassword());
    }
}
