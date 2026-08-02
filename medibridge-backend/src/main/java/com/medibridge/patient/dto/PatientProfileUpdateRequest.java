package com.medibridge.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Email is set-once, not editable: it is a login identifier, and changing one
 * would need re-verification this screen does not do.
 *
 * <p>Optional here because only one caller ever supplies it - a phone-first
 * account, which signed up without an email and is filling one in. On every
 * other account the field is already set and this is ignored.
 */
public record PatientProfileUpdateRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100)
        @JsonProperty("full_name")
        String fullName,

        @Email(message = "Enter a valid email address")
        @Size(max = 100)
        String email,

        @NotBlank(message = "Phone number is required")
        @Size(max = 20)
        String phone,

        @Size(max = 20)
        @JsonProperty("another_number")
        String anotherNumber,

        String address,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        @JsonProperty("date_of_birth")
        LocalDate dateOfBirth,

        @NotBlank
        @Pattern(regexp = "Male|Female|Other")
        String gender,

        @NotBlank
        @Pattern(regexp = "A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-", message = "Invalid blood group")
        @JsonProperty("blood_group")
        String bloodGroup
) {
}
