package com.medibridge.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.util.List;

/** Mirrors the state RateExperience.jsx collects. */
public record RatingRequest(

        @NotNull(message = "Appointment is required")
        @JsonProperty("appointment_id")
        Integer appointmentId,

        @NotNull(message = "Please select a star rating")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Short stars,

        @NotBlank(message = "Overall experience is required")
        @Pattern(regexp = "Excellent|Good|Okay|Poor",
                 message = "Overall experience must be Excellent, Good, Okay or Poor")
        @JsonProperty("overall_experience")
        String overallExperience,

        /** Multi-select - "Bedside Manner", "Clear Explanations", ... */
        @JsonProperty("what_stood_out")
        List<String> whatStoodOut,

        @Size(max = 2000)
        @JsonProperty("review_text")
        String reviewText
) {
}
