package com.medibridge.appointment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Flattened for the frontend: PatientOverview.jsx and PatientAppointments.jsx
 * read {@code a.doctor} and {@code a.specialization} as plain strings, and show
 * date and time as separate fields.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppointmentResponse(

        @JsonProperty("appointment_id")
        Integer appointmentId,

        /** Doctor's display name - a string, not an object. */
        String doctor,

        @JsonProperty("doctor_id")
        String doctorId,

        String specialization,

        /** Patient's display name, used by the doctor and admin views. */
        String patient,

        @JsonProperty("patient_id")
        Integer patientId,

        Integer age,

        @JsonProperty("appointment_date")
        String appointmentDate,

        String time,

        String type,

        String status,

        String reason,

        @JsonProperty("consultation_fee")
        BigDecimal consultationFee,

        /**
         * The raw room URL is deliberately NOT sent in list responses — it is a
         * bearer secret. The frontend shows a Join button based on {@code canJoin}
         * and fetches the actual link from GET /appointments/{id}/join only when
         * the user clicks, inside the allowed window.
         */
        @JsonProperty("meeting_link")
        String meetingLink,

        /** True only while the join window is open (≈15 min before → ~1h after). */
        @JsonProperty("can_join")
        Boolean canJoin,

        /** When the room opens, so the UI can show "Available at 09:45 AM". */
        @JsonProperty("join_from")
        String joinFrom
) {
}
