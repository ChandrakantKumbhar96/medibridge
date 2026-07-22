package com.medibridge.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Field names mirror the state object in DoctorRegisterForm.jsx exactly. */
public record DoctorRegisterRequest(

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

        /** Name from the dropdown, resolved to a specialization row on save. */
        @NotBlank(message = "Specialization is required")
        String specialization,

        @NotBlank(message = "License number is required")
        @Size(max = 50)
        @JsonProperty("license_number")
        String licenseNumber,

        @NotNull(message = "Experience is required")
        @Min(value = 0, message = "Experience cannot be negative")
        @Max(value = 70, message = "Experience looks incorrect")
        @JsonProperty("experience_years")
        Integer experienceYears,

        @NotNull(message = "Consultation fee is required")
        @DecimalMin(value = "0.0", message = "Fee cannot be negative")
        @JsonProperty("consultation_fee")
        BigDecimal consultationFee,

        @NotNull(message = "Consultation duration is required")
        @Min(value = 5) @Max(value = 240)
        @JsonProperty("consultation_duration_min")
        Integer consultationDurationMin,

        @Size(max = 2000)
        String bio
) {
}
