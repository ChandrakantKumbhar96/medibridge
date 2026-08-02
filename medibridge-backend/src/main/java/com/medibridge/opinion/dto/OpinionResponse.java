package com.medibridge.opinion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpinionResponse(

        @JsonProperty("opinion_id")
        Integer opinionId,

        @JsonProperty("appointment_id")
        Integer appointmentId,

        String patient,

        String doctor,

        String specialization,

        @JsonProperty("original_diagnosis")
        String originalDiagnosis,

        String findings,

        @JsonProperty("agrees_with_original")
        Boolean agreesWithOriginal,

        /**
         * The verdict as display text - "Agrees with the original diagnosis" or
         * "Differs from the original diagnosis" - so every surface words it the
         * same way. A boolean rendered independently in three places drifts into
         * three different phrasings, and this one carries clinical weight.
         */
        String verdict,

        String recommendation,

        @JsonProperty("suggested_tests")
        String suggestedTests,

        @JsonProperty("issued_on")
        String issuedOn,

        @JsonProperty("pdf_url")
        String pdfUrl
) {
}
