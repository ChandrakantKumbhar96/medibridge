package com.medibridge.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Only the signed credential is accepted. Email and name are deliberately not
 * fields here - taking them from the request body would let a caller claim any
 * identity. They are read from the verified token payload instead.
 */
public record GoogleLoginRequest(

        @NotBlank(message = "Google credential is required")
        String credential,

        /** Only "patient" is supported; see AuthService.loginWithGoogle. */
        String role
) {
}
