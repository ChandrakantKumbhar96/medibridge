package com.medibridge.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** Backs the chat-service's get_refund_status tool - see spring_client.py. */
public record RefundStatusResponse(

        @JsonProperty("refund_amount")
        BigDecimal refundAmount,

        @JsonProperty("refunded_at")
        String refundedAt,

        @JsonProperty("refund_reason")
        String refundReason
) {
}
