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

        /**
         * Who is being seen, used by the doctor and admin views - the dependent's
         * name when the visit is for one, otherwise the account holder's.
         *
         * <p>Deliberately the subject and not the payer: a doctor's list showing
         * the parent for a paediatric slot is a list that sends the wrong person
         * into the room.
         */
        String patient,

        /** The owning account, which is still the payer and the canceller. */
        @JsonProperty("patient_id")
        Integer patientId,

        /** Age of the person being seen, not of the account holder. */
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
        String joinFrom,

        /**
         * True when this patient may cancel with a full refund regardless of how
         * close the appointment is, because the doctor moved it.
         *
         * <p>Sent so the UI can stop threatening a cancellation fee it will not
         * charge. Advisory only - the refund is decided server-side in
         * {@code cancelAsPatient}, never from this flag.
         */
        @JsonProperty("free_cancellation")
        Boolean freeCancellation,

        /** PATIENT, DOCTOR or BOTH once closed as a no-show; null otherwise. */
        @JsonProperty("no_show_by")
        String noShowBy,

        /**
         * Place in the doctor's queue for today, 1 being the consultation
         * currently in the room.
         *
         * <p>Null on every appointment that is not a confirmed booking for
         * today - a position for next week is a number nobody can act on, and
         * it would be stale before the page finished rendering.
         */
        @JsonProperty("queue_position")
        Integer queuePosition,

        /** Minutes from now until this patient is likely to be seen. */
        @JsonProperty("eta_minutes")
        Integer etaMinutes,

        /**
         * How much later than its slot time this consultation is now expected
         * to start, derived from observed join times rather than from anything
         * either party declared.
         *
         * <p>Omitted when it is on time: NON_NULL drops it from the JSON, so the
         * UI reads "on time" from the field's absence instead of having to
         * special-case a zero.
         */
        @JsonProperty("delay_minutes")
        Integer delayMinutes,

        /**
         * Set only when the visit is for a dependent; NON_NULL drops it entirely
         * for an ordinary self-booking, so the UI reads "for me" from its absence
         * rather than comparing ids.
         */
        @JsonProperty("family_member_id")
        Integer familyMemberId,

        /** Child, Spouse, Parent, Sibling, Other - null for a self-booking. */
        @JsonProperty("booked_for")
        String bookedFor,

        /**
         * The account holder's name, sent only for a dependent's visit so the
         * doctor can see who to call - the child in the chair is not the number
         * on file.
         */
        @JsonProperty("account_holder")
        String accountHolder,

        /**
         * Last moment a free revisit with this doctor may be booked off this
         * consultation, as ISO local date-time.
         *
         * <p>Sent on the completed appointment itself so the list screen can
         * decide whether to offer the button without a second call per row.
         * NON_NULL drops it entirely once the revisit is spent, the window has
         * closed, or the consultation never earned one - so its presence is the
         * whole signal, and the UI never has to reimplement the policy.
         *
         * <p>Advisory, like {@code free_cancellation}: eligibility is decided
         * again server-side in {@code bookFollowUp}, never from this field.
         */
        @JsonProperty("follow_up_eligible_until")
        String followUpEligibleUntil
) {
}
