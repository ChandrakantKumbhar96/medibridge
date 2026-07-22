package com.medibridge.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(

        @JsonProperty("transaction_id")
        Integer transactionId,

        @JsonProperty("transaction_ref")
        String transactionRef,

        @JsonProperty("appointment_id")
        Integer appointmentId,

        BigDecimal amount,

        @JsonProperty("payment_method")
        String paymentMethod,

        String status,

        @JsonProperty("processed_at")
        String processedAt,

        String doctor,

        @JsonProperty("invoice_url")
        String invoiceUrl
) {
}
