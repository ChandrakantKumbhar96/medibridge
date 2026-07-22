package com.medibridge.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * No amount field, deliberately. The amount is read from the doctor's
 * consultation fee on the server - accepting it from the client would let a
 * user pay whatever they liked.
 */
public record CreateOrderRequest(

        @NotNull(message = "Appointment is required")
        @JsonProperty("appointment_id")
        Integer appointmentId
) {
}
