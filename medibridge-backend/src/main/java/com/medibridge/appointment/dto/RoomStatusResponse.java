package com.medibridge.appointment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Who is in the consultation room, polled by the patient's waiting room.
 *
 * <p>Deliberately separate from {@code AppointmentResponse}: this is fetched
 * every few seconds while someone waits, so it carries only what changes and
 * none of the joins that the full appointment payload needs.
 *
 * <p><b>It never carries the room URL.</b> The link stays behind
 * {@code GET /appointments/{id}/join}, which is the one place ownership, payment
 * and the join window are all enforced together - and the one place attendance
 * is recorded. A polling endpoint that handed out the link would either leak it
 * or stamp attendance once per poll.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomStatusResponse(

        @JsonProperty("appointment_id")
        Integer appointmentId,

        /**
         * True once the doctor has asked for the room link.
         *
         * <p>This is "the doctor opened the door", not "the doctor is on
         * camera" - the platform has no visibility inside the video provider's
         * room. It is the strongest signal available without integrating the
         * provider, and it is the same timestamp the no-show sweep judges on.
         */
        @JsonProperty("doctor_joined")
        Boolean doctorJoined,

        /** True once this patient has asked for the link - i.e. they turned up. */
        @JsonProperty("patient_joined")
        Boolean patientJoined,

        /** Whether the join window is currently open. */
        @JsonProperty("can_join")
        Boolean canJoin,

        /** When the room opens, so the UI can say "Opens at 09:45 AM". */
        @JsonProperty("join_from")
        String joinFrom,

        /**
         * Frontend-facing status. A consultation cancelled while the patient sat
         * in the waiting room has to stop them waiting for someone who is never
         * coming.
         */
        String status
) {
}
