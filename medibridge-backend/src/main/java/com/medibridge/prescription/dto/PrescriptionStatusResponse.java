package com.medibridge.prescription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Backs the chat-service's get_prescription_status tool - see spring_client.py. */
public record PrescriptionStatusResponse(

        @JsonProperty("prescription_id")
        Integer prescriptionId,

        @JsonProperty("date_issued")
        String dateIssued,

        @JsonProperty("doctor_name")
        String doctorName
) {
}
