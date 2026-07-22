package com.medibridge.appointment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookAppointmentRequest(

        @NotBlank(message = "Doctor is required")
        @JsonProperty("doctor_id")
        String doctorId,

        /**
         * The chosen slot from GET /doctors/{id}/slots. Taking a slot id rather
         * than a raw date+time is what lets the unique constraint on
         * appointment.schedule_id prevent double booking.
         */
        @NotNull(message = "Please select a time slot")
        @JsonProperty("schedule_id")
        Integer scheduleId,

        @Size(max = 50)
        @JsonProperty("consult_type")
        String consultType,

        String reason
) {
}
