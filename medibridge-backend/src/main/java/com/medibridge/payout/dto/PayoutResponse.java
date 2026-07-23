package com.medibridge.payout.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PayoutResponse(

        @JsonProperty("payout_id")
        Integer payoutId,

        @JsonProperty("doctor_id")
        String doctorId,

        String doctor,

        @JsonProperty("period_start")
        String periodStart,

        @JsonProperty("period_end")
        String periodEnd,

        Integer consultations,

        @JsonProperty("gross_amount")
        BigDecimal grossAmount,

        BigDecimal commission,

        @JsonProperty("net_amount")
        BigDecimal netAmount,

        String status,

        @JsonProperty("payout_ref")
        String payoutRef,

        @JsonProperty("created_at")
        String createdAt,

        @JsonProperty("paid_at")
        String paidAt
) {
}
