package com.medibridge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Step one of phone login. Accepts the number however the patient types it -
 * spaces, brackets, a leading zero or a +91 - because the server normalises it
 * anyway and rejecting "+91 90000 11111" for its spaces would be theatre.
 */
public record PhoneOtpRequest(

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "[+0-9][0-9 ()+-]{7,19}",
                 message = "Enter a valid mobile number")
        String phone
) {
}
