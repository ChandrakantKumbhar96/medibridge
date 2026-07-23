package com.medibridge.payout.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EarningResponse(

        @JsonProperty("earning_id")
        Integer earningId,

        @JsonProperty("appointment_id")
        Integer appointmentId,

        String patient,

        @JsonProperty("consultation_date")
        String consultationDate,

        /** Consultation fee - excludes the platform fee the patient also paid. */
        @JsonProperty("gross_amount")
        BigDecimal grossAmount,

        @JsonProperty("commission_rate")
        BigDecimal commissionRate,

        @JsonProperty("commission_amount")
        BigDecimal commissionAmount,

        @JsonProperty("net_amount")
        BigDecimal netAmount,

        String status,

        @JsonProperty("payout_id")
        Integer payoutId,

        @JsonProperty("earned_at")
        String earnedAt
) {
}
