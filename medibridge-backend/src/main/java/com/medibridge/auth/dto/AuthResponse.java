package com.medibridge.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Shape required by features/auth/authSlice.js: it persists {@code token} to
 * localStorage as {@code mb_token} and {@code user} as {@code mb_user}, and the
 * route guards read {@code user.role}.
 */
public record AuthResponse(

        String token,

        @JsonProperty("refresh_token")
        String refreshToken,

        User user
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record User(
            String id,
            String name,
            String email,
            String role,

            /** Doctor only - the topbar shows it next to the name. */
            String specialization,

            /** Admin only. */
            String title,

            @JsonProperty("consultation_fee")
            BigDecimal consultationFee,

            /** Doctor only: 'pending' until an admin approves the account. */
            String status
    ) {
    }
}
