package com.medibridge.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RatingResponse(

        @JsonProperty("rating_id")
        Integer ratingId,

        @JsonProperty("appointment_id")
        Integer appointmentId,

        @JsonProperty("doctor_id")
        String doctorId,

        String doctor,

        @JsonProperty("patient_name")
        String patientName,

        Short stars,

        @JsonProperty("overall_experience")
        String overallExperience,

        @JsonProperty("what_stood_out")
        List<String> whatStoodOut,

        @JsonProperty("review_text")
        String reviewText,

        @JsonProperty("created_at")
        String createdAt
) {
}
