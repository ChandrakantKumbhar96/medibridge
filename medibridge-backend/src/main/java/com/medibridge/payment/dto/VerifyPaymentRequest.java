package com.medibridge.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** The three values Razorpay's checkout handler returns on success. */
public record VerifyPaymentRequest(

        @NotBlank
        @JsonProperty("razorpay_order_id")
        String razorpayOrderId,

        @NotBlank
        @JsonProperty("razorpay_payment_id")
        String razorpayPaymentId,

        @NotBlank
        @JsonProperty("razorpay_signature")
        String razorpaySignature
) {
}
