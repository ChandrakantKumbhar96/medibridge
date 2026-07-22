package com.medibridge.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** Everything the Razorpay checkout widget needs. The key secret is not here. */
public record OrderResponse(

        @JsonProperty("order_id")
        String orderId,

        @JsonProperty("razorpay_key_id")
        String razorpayKeyId,

        BigDecimal amount,

        /** Paise - what the checkout widget expects. */
        @JsonProperty("amount_paise")
        long amountPaise,

        String currency,

        @JsonProperty("transaction_id")
        Integer transactionId,

        @JsonProperty("appointment_id")
        Integer appointmentId,

        String doctor,

        @JsonProperty("patient_name")
        String patientName,

        @JsonProperty("patient_email")
        String patientEmail,

        @JsonProperty("patient_phone")
        String patientPhone
) {
}
