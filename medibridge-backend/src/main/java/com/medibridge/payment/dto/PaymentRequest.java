package com.medibridge.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Deliberately carries no card number, CVV, or expiry.
 *
 * <p>Payments are simulated: a real integration would hand those details
 * straight to the gateway's hosted form and receive a token back, so they must
 * never reach or be stored by this application.
 */
public record PaymentRequest(

        @NotNull(message = "Appointment is required")
        @JsonProperty("appointment_id")
        Integer appointmentId,

        @NotBlank(message = "Payment method is required")
        @Pattern(regexp = "CARD|UPI|NETBANKING|WALLET",
                 message = "Payment method must be CARD, UPI, NETBANKING or WALLET")
        @JsonProperty("payment_method")
        String paymentMethod
) {
}
