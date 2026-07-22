package com.medibridge.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Field names mirror the state object in PatientRegisterForm.jsx exactly. */
public record PatientRegisterRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100)
        @JsonProperty("full_name")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 100)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

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

        @NotBlank(message = "Gender is required")
        @Pattern(regexp = "Male|Female|Other", message = "Gender must be Male, Female or Other")
        String gender,

        @NotBlank(message = "Blood group is required")
        @Pattern(regexp = "A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-",
                 message = "Invalid blood group")
        @JsonProperty("blood_group")
        String bloodGroup,

        @JsonProperty("reason_of_consult")
        String reasonOfConsult
) {
}
