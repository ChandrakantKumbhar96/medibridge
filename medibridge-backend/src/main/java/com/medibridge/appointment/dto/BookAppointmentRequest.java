package com.medibridge.appointment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookAppointmentRequest(

        @NotBlank(message = "Doctor is required")
        @JsonProperty("doctor_id")
        String doctorId,

        /**
         * The chosen slot from GET /doctors/{id}/slots. Taking a slot id rather
         * than a raw date+time is what lets the unique constraint on
         * appointment.schedule_id prevent double booking.
         */
        @NotNull(message = "Please select a time slot")
        @JsonProperty("schedule_id")
        Integer scheduleId,

        @Size(max = 50)
        @JsonProperty("consult_type")
        String consultType,

        String reason,

        /**
         * Who the visit is for. Null - the common case - means the account holder
         * themselves.
         *
         * <p>Unvalidated here on purpose: "does this dependent belong to the
         * caller" is not a shape constraint, and answering it needs the token's
         * owner id, which a bean validator never sees. It is resolved in
         * {@code FamilyMemberService.requireOwned} and backed by a composite
         * foreign key.
         */
        @JsonProperty("family_member_id")
        Integer familyMemberId
) {
}
