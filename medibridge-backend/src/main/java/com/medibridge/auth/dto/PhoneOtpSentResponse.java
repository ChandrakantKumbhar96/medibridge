package com.medibridge.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The answer to "send me a code", and it says the same thing whether or not the
 * number has an account. Anything conditional here - a different message, a
 * different status, even a noticeably different response time - would turn the
 * endpoint into a way to ask "is this person a MediBridge patient?".
 *
 * <p>The two numbers are configuration the login screen needs to render itself:
 * how many boxes to draw, and how long to disable "Resend" for. Hardcoding
 * either in the frontend means the day an admin retunes them, the UI lies.
 */
public record PhoneOtpSentResponse(

        String message,

        @JsonProperty("code_length")
        int codeLength,

        @JsonProperty("resend_in_seconds")
        int resendInSeconds,

        @JsonProperty("expires_in_seconds")
        int expiresInSeconds
) {
}
