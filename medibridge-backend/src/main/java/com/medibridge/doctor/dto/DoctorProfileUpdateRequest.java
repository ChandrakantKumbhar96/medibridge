package com.medibridge.doctor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Backs the Doctor Settings screen. Email and license are intentionally not editable. */
public record DoctorProfileUpdateRequest(

        @NotBlank @Size(max = 100)
        @JsonProperty("full_name")
        String fullName,

        @NotBlank @Size(max = 20)
        String phone,

        @NotBlank
        String specialization,

        @NotNull @Min(0) @Max(70)
        @JsonProperty("experience_years")
        Integer experienceYears,

        @NotNull @DecimalMin("0.0")
        @JsonProperty("consultation_fee")
        BigDecimal consultationFee,

        @NotNull @Min(5) @Max(240)
        @JsonProperty("consultation_duration_min")
        Integer consultationDurationMin,

        @Size(max = 2000)
        String bio
) {
}
