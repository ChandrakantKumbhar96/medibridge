package com.medibridge.prescription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PrescriptionResponse(

        @JsonProperty("prescription_id")
        Integer prescriptionId,

        @JsonProperty("appointment_id")
        Integer appointmentId,

        @JsonProperty("patient_name")
        String patientName,

        @JsonProperty("doctor_name")
        String doctorName,

        String specialization,

        String diagnosis,

        String notes,

        String advice,

        @JsonProperty("date_issued")
        String dateIssued,

        @JsonProperty("follow_up_date")
        String followUpDate,

        List<Item> medicines,

        @JsonProperty("pdf_url")
        String pdfUrl
) {

    public record Item(
            @JsonProperty("medicine_name") String medicineName,
            String dosage,
            String frequency,
            String duration,
            String instructions
    ) {
    }
}
