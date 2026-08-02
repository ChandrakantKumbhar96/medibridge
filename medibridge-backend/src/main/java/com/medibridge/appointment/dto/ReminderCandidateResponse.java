package com.medibridge.appointment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One row of {@code GET /internal/appointments/reminder-candidates} - just
 * enough for the notify-service to compose and send the SMS itself, without a
 * second round trip per appointment.
 */
public record ReminderCandidateResponse(

        @JsonProperty("appointment_id")
        Integer appointmentId,

        /** E.164, or null when the patient has no resolvable number. */
        @JsonProperty("patient_phone")
        String patientPhone,

        @JsonProperty("patient_name")
        String patientName,

        @JsonProperty("doctor_name")
        String doctorName,

        @JsonProperty("appointment_date")
        String appointmentDate
) {
}
