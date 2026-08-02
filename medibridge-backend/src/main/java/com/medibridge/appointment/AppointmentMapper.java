package com.medibridge.appointment;

import com.medibridge.appointment.dto.AppointmentResponse;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.patient.entity.FamilyMember;
import com.medibridge.patient.entity.Patient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public final class AppointmentMapper {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Locale.ENGLISH pinned, then upper-cased: Java 21's CLDR data renders the
     * am/pm marker lowercase, but the frontend's mock data and layout expect
     * "02:30 PM".
     */
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH);

    private AppointmentMapper() {
    }

    /**
     * @param forParticipant true for the patient's or treating doctor's own view
     *                       (they get join status); false for admin listings.
     *
     * <p>The raw meeting URL is never placed in a list response — it would be a
     * bearer secret sitting in every payload. Instead the participant gets a
     * {@code canJoin} flag and the window opening time; the actual link is
     * fetched on click from the dedicated join endpoint.
     */
    public static AppointmentResponse toDto(Appointment a, boolean forParticipant) {
        return toDto(a, forParticipant, null);
    }

    /**
     * @param queue this appointment's live queue standing, or null when it has
     *              none - which is every appointment that is not a confirmed
     *              booking for today. See {@link LiveQueueService}.
     */
    public static AppointmentResponse toDto(Appointment a, boolean forParticipant,
                                            LiveQueueService.QueueView queue) {
        return toDto(a, forParticipant, queue, null);
    }

    /**
     * @param followUpEligibleUntil when a free revisit off this consultation may
     *                              still be booked, or null when it may not.
     *                              Passed in rather than derived here: it needs
     *                              the configured window and the knowledge that
     *                              no follow-up has been taken yet, neither of
     *                              which belongs in a static mapper.
     */
    public static AppointmentResponse toDto(Appointment a, boolean forParticipant,
                                            LiveQueueService.QueueView queue,
                                            LocalDateTime followUpEligibleUntil) {
        LocalDateTime when = a.getAppointmentDate();
        Patient p = a.getPatient();
        FamilyMember subject = a.getFamilyMember();

        boolean hasRoom = forParticipant && a.getMeetingLink() != null;
        Boolean canJoin = hasRoom ? a.isMeetingJoinable() : null;
        String joinFrom = hasRoom && a.getMeetingJoinFrom() != null
                ? a.getMeetingJoinFrom().format(TIME).toUpperCase(java.util.Locale.ENGLISH)
                : null;

        return new AppointmentResponse(
                a.getId(),
                a.getDoctor().getFullName(),
                a.getDoctor().getId(),
                a.getDoctor().getSpecialization().getName(),
                a.subjectName(),
                p.getId(),
                ageOf(a.subjectDateOfBirth()),
                when.format(DATE),
                when.format(TIME).toUpperCase(java.util.Locale.ENGLISH),
                a.getConsultType(),
                a.getStatus().toFrontend(),
                a.getReason(),
                feeOf(a),
                null,          // raw link never sent in lists — see join endpoint
                canJoin,
                joinFrom,
                // NON_NULL on the record means false would be dropped from the
                // JSON entirely, so only the true case is worth sending.
                a.wasMovedByDoctor() ? Boolean.TRUE : null,
                a.getNoShowBy() == null ? null : a.getNoShowBy().name(),
                queue == null ? null : queue.position(),
                queue == null ? null : queue.etaMinutes(),
                // Zero is "on time", and NON_NULL would drop a false-y field
                // anyway - so absence is the signal, not a 0 the UI must test.
                queue == null || queue.delayMinutes() == 0 ? null : queue.delayMinutes(),
                subject == null ? null : subject.getId(),
                subject == null ? null : subject.getRelation().name(),
                subject == null ? null : p.getFullName(),
                // ISO so the browser can compare it against now() directly; the
                // display fields above are formatted for humans and would need
                // parsing rules the UI should not own.
                followUpEligibleUntil == null ? null
                        : followUpEligibleUntil.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    /**
     * What this appointment actually cost, not what the doctor charges today.
     *
     * <p>{@code booked_fee} is snapshotted at booking precisely so a later price
     * change cannot alter what an existing patient owes - reading the doctor's
     * live fee here threw that guarantee away at the last step, and showed the
     * patient a number that disagreed with their receipt. A second opinion,
     * billed at a premium, made the gap visible: charged 1200, displayed 800.
     *
     * <p>Falls back to the doctor's fee only for rows predating the snapshot
     * column.
     */
    private static BigDecimal feeOf(Appointment a) {
        return a.getBookedFee() != null
                ? a.getBookedFee()
                : a.getDoctor().getConsultationFee();
    }

    public static Integer ageOf(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public static String describe(Appointment a) {
        return a.getAppointmentDate().format(DATE) + " at "
                + a.getAppointmentDate().format(TIME).toUpperCase(java.util.Locale.ENGLISH);
    }
}
