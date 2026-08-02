package com.medibridge.appointment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * The soonest open slot found by {@code GET /appointments/next-available}.
 *
 * <p>A preview, not a booking - {@code scheduleId} is handed straight back
 * into the existing {@link BookAppointmentRequest} to confirm, so no new
 * booking path or appointment status exists for this feature.
 */
public record NextAvailableResponse(
        @JsonProperty("doctor_id") String doctorId,
        @JsonProperty("doctor_name") String doctorName,
        @JsonProperty("specialization") String specialization,
        @JsonProperty("schedule_id") Integer scheduleId,
        @JsonProperty("available_date") String availableDate,
        @JsonProperty("start_time") String startTime,
        @JsonProperty("fee") BigDecimal fee,
        @JsonProperty("wait_minutes") Integer waitMinutes
) {
}
