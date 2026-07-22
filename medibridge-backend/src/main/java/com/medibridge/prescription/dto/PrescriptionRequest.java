package com.medibridge.prescription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/**
 * A doctor recording a consultation and issuing a prescription in one call -
 * they happen together clinically, so splitting them would let one exist
 * without the other.
 */
public record PrescriptionRequest(

        @NotNull(message = "Appointment is required")
        @JsonProperty("appointment_id")
        Integer appointmentId,

        @NotBlank(message = "Diagnosis is required")
        @Size(max = 255)
        String diagnosis,

        String notes,

        @JsonProperty("follow_up_date")
        LocalDate followUpDate,

        String advice,

        @NotEmpty(message = "At least one medicine is required")
        @Valid
        List<Item> medicines
) {

    public record Item(

            @NotBlank(message = "Medicine name is required")
            @Size(max = 150)
            @JsonProperty("medicine_name")
            String medicineName,

            @NotBlank(message = "Dosage is required")
            @Size(max = 50)
            String dosage,

            @NotBlank(message = "Frequency is required")
            @Size(max = 50)
            String frequency,

            @NotBlank(message = "Duration is required")
            @Size(max = 50)
            String duration,

            @Size(max = 255)
            String instructions
    ) {
    }
}
