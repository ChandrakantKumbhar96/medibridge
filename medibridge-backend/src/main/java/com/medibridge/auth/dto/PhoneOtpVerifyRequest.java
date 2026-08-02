package com.medibridge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Step two. The number is sent again rather than held in a server-side session:
 * the code is only ever valid for the number it was texted to, so the pair is
 * the credential.
 */
public record PhoneOtpVerifyRequest(

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "[+0-9][0-9 ()+-]{7,19}",
                 message = "Enter a valid mobile number")
        String phone,

        /* Shape only, and the length is not in the message: the code length is
           configurable, and a validation error is a poor place to publish it. */
        @NotBlank(message = "Enter the code we sent you")
        @Pattern(regexp = "\\d{4,8}", message = "Enter the code we sent you")
        String code
) {
}
