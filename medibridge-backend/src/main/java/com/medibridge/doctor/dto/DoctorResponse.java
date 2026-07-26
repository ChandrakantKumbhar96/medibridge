package com.medibridge.doctor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Shape consumed by FindDoctors.jsx and the booking wizard. Field names are
 * snake_case to match what those components already read (d.full_name,
 * d.consultation_fee, d.experience_years).
 */
public record DoctorResponse(

        @JsonProperty("doctor_id")
        String doctorId,

        @JsonProperty("full_name")
        String fullName,

        String specialization,

        String email,

        String phone,

        @JsonProperty("license_number")
        String licenseNumber,

        @JsonProperty("experience_years")
        Integer experienceYears,

        @JsonProperty("consultation_fee")
        BigDecimal consultationFee,

        @JsonProperty("consultation_duration_min")
        Integer consultationDurationMin,

        String bio,

        String qualifications,

        String languages,

        Double rating,

        @JsonProperty("rating_count")
        Integer ratingCount,

        /** Frontend renders an "Available"/"Unavailable" badge from this. */
        Boolean available,

        String status
) {
}
