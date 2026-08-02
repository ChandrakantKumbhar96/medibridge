package com.medibridge.appointment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Backs the chat-service's get_next_appointment tool - see spring_client.py. */
public record NextAppointmentResponse(

        @JsonProperty("with_name")
        String withName,

        @JsonProperty("appointment_date")
        String appointmentDate,

        @JsonProperty("status")
        String status
) {
}
