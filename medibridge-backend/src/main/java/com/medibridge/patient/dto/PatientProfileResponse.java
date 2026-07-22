package com.medibridge.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Field names match PatientSettings.jsx and the register form. */
public record PatientProfileResponse(

        @JsonProperty("patient_id")
        Integer patientId,

        @JsonProperty("full_name")
        String fullName,

        String email,

        String phone,

        @JsonProperty("another_number")
        String anotherNumber,

        String address,

        @JsonProperty("date_of_birth")
        String dateOfBirth,

        String gender,

        @JsonProperty("blood_group")
        String bloodGroup,

        @JsonProperty("reason_of_consult")
        String reasonOfConsult,

        String status
) {
}
