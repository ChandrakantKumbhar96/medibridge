package com.medibridge.opinion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What the reviewing specialist writes up.
 *
 * <p>Everything except {@code suggestedTests} is required. A second opinion that
 * omits the verdict, or the reasoning behind it, is not a shorter opinion - it is
 * an unusable one, and the patient has already paid a premium for it.
 */
public record OpinionRequest(

        @NotNull(message = "Appointment is required")
        @JsonProperty("appointment_id")
        Integer appointmentId,

        @NotBlank(message = "The diagnosis under review is required")
        @Size(max = 2000)
        @JsonProperty("original_diagnosis")
        String originalDiagnosis,

        @NotBlank(message = "Your findings are required")
        @Size(max = 4000)
        String findings,

        /**
         * Boxed and {@code @NotNull} rather than a primitive: a missing verdict
         * would otherwise arrive silently as false, publishing a disagreement
         * the specialist never expressed.
         */
        @NotNull(message = "State whether you agree with the original diagnosis")
        @JsonProperty("agrees_with_original")
        Boolean agreesWithOriginal,

        @NotBlank(message = "A recommendation is required")
        @Size(max = 4000)
        String recommendation,

        @Size(max = 2000)
        @JsonProperty("suggested_tests")
        String suggestedTests
) {
}
