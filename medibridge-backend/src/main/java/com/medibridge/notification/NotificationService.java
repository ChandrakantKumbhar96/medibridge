package com.medibridge.notification;

import com.medibridge.appointment.entity.Appointment;
import com.medibridge.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Composes and records every outbound message.
 *
 * <p>Each send is written to the {@code notification} table first, then handed
 * to {@link EmailService}. Two reasons: an examiner (or an auditor) can see
 * exactly what the platform told a patient, and the unique key makes reminders
 * idempotent so a re-run cannot double-send.
 *
 * <p>REQUIRES_NEW throughout: a notification is a side effect of a business
 * action, never a reason to roll one back. A booking must not fail because the
 * mail server was slow.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy 'at' hh:mm a");

    private final NotificationRepository repository;
    private final EmailService emailService;
    private final SmsService smsService;

    // ------------------------------------------------------------ templates

    public void sendBookingPending(Appointment a) {
        deliver(a, Notification.Type.BOOKING_PENDING,
                "Complete your MediBridge booking",
                """
                Hello %s,

                Your slot with %s on %s is held for a short time while you
                complete payment.

                Amount payable: Rs. %s
                (Consultation Rs. %s + platform fee Rs. %s)

                The slot is released automatically if payment is not completed.

                - MediBridge
                """.formatted(
                        a.getPatient().getFullName(),
                        a.getDoctor().getFullName(),
                        a.getAppointmentDate().format(WHEN),
                        a.getTotalAmount(), a.getBookedFee(), a.getPlatformFee()));
    }

    public void sendBookingConfirmed(Appointment a) {
        deliver(a, Notification.Type.BOOKING_CONFIRMED,
                "Your MediBridge consultation is confirmed",
                """
                Hello %s,

                Your consultation with %s is CONFIRMED for %s.

                Amount paid: Rs. %s

                When it is time, open MediBridge, go to My Appointments, and press
                Join. The video room opens shortly before your appointment and runs
                in your browser - no account or installation needed.

                - MediBridge
                """.formatted(
                        a.getPatient().getFullName(),
                        a.getDoctor().getFullName(),
                        a.getAppointmentDate().format(WHEN),
                        a.getTotalAmount()));

        deliverSms(a, Notification.Type.BOOKING_CONFIRMED,
                "MediBridge: your consultation with %s on %s is CONFIRMED. Open the app and press Join at your appointment time."
                        .formatted(a.getDoctor().getFullName(),
                                a.getAppointmentDate().format(WHEN)));
    }

    public void sendReminder(Appointment a) {
        deliver(a, Notification.Type.REMINDER_24H,
                "Reminder: your MediBridge consultation is tomorrow",
                """
                Hello %s,

                A reminder that your consultation with %s is on %s.

                Open MediBridge and press Join on the appointment when it is time -
                the video room opens shortly before your slot.

                If you can no longer attend, please cancel from the app so the
                slot can be offered to someone else.

                - MediBridge
                """.formatted(
                        a.getPatient().getFullName(),
                        a.getDoctor().getFullName(),
                        a.getAppointmentDate().format(WHEN)));

        deliverSms(a, Notification.Type.REMINDER_24H,
                "MediBridge reminder: consultation with %s on %s. Open the app and press Join at your slot time."
                        .formatted(a.getDoctor().getFullName(),
                                a.getAppointmentDate().format(WHEN)));
    }

    public void sendCancelled(Appointment a, BigDecimal refundAmount, String reason) {
        String refundLine = refundAmount == null || refundAmount.signum() == 0
                ? "No payment had been taken, so there is nothing to refund."
                : "A refund of Rs. %s has been issued and should reach your account in 5-7 working days."
                        .formatted(refundAmount);

        deliver(a, Notification.Type.APPOINTMENT_CANCELLED,
                "Your MediBridge appointment was cancelled",
                """
                Hello %s,

                Your appointment with %s on %s has been cancelled.
                %s

                %s

                - MediBridge
                """.formatted(
                        a.getPatient().getFullName(),
                        a.getDoctor().getFullName(),
                        a.getAppointmentDate().format(WHEN),
                        reason == null ? "" : "Reason: " + reason,
                        refundLine));

        String refundSms = refundAmount == null || refundAmount.signum() == 0
                ? "" : " A refund of Rs. " + refundAmount + " has been issued.";
        deliverSms(a, Notification.Type.APPOINTMENT_CANCELLED,
                ("MediBridge: your appointment with %s on %s was cancelled." + refundSms)
                        .formatted(a.getDoctor().getFullName(),
                                a.getAppointmentDate().format(WHEN)));
    }

    /**
     * Tells the patient an appointment was written off as unattended.
     *
     * <p>Deliberately not worded as a cancellation. Nobody cancelled it, and a
     * patient who reads "cancelled" next to "no refund" concludes they were
     * charged for a cancellation - which is the complaint this wording exists
     * to prevent. The message names what happened and what it cost.
     *
     * @param by             PATIENT, DOCTOR or BOTH
     * @param refundAmount   what was actually returned; zero for a patient no-show
     */
    public void sendNoShow(Appointment a, String by, BigDecimal refundAmount) {
        boolean patientMissedIt = "PATIENT".equals(by);
        boolean refunded = refundAmount != null && refundAmount.signum() > 0;

        String explanation = patientMissedIt
                ? """
                  Our records show the consultation room was not opened from your
                  side, so the appointment has been closed as missed. As set out
                  at booking, a missed consultation is not refunded - the doctor
                  held the time.
                  """
                : "DOCTOR".equals(by)
                        ? "The doctor was unable to join, so you are not being charged for it."
                        : "The consultation did not take place, so you are not being charged for it.";

        String refundLine = refunded
                ? "A refund of Rs. %s has been issued and should reach your account in 5-7 working days."
                        .formatted(refundAmount)
                : patientMissedIt
                        ? "You can book a new appointment whenever suits you."
                        : "Any amount paid is being returned to you.";

        deliver(a, Notification.Type.APPOINTMENT_NO_SHOW,
                patientMissedIt
                        ? "You missed your MediBridge consultation"
                        : "Your MediBridge consultation did not take place",
                """
                Hello %s,

                Your appointment with %s on %s did not go ahead.

                %s

                %s

                - MediBridge
                """.formatted(
                        a.getPatient().getFullName(),
                        a.getDoctor().getFullName(),
                        a.getAppointmentDate().format(WHEN),
                        explanation.strip(),
                        refundLine));

        deliverSms(a, Notification.Type.APPOINTMENT_NO_SHOW,
                patientMissedIt
                        ? "MediBridge: you missed your consultation with %s on %s. A missed consultation is not refunded."
                                .formatted(a.getDoctor().getFullName(),
                                        a.getAppointmentDate().format(WHEN))
                        : "MediBridge: your consultation with %s on %s did not take place. %s"
                                .formatted(a.getDoctor().getFullName(),
                                        a.getAppointmentDate().format(WHEN), refundLine));
    }

    /**
     * Not de-duplicated by the usual key alone: an appointment can legitimately
     * be rescheduled more than once, so the count is folded into the type to
     * keep each move notifiable.
     */
    public void sendRescheduled(Appointment a, String by) {
        String who = "DOCTOR".equals(by)
                ? a.getDoctor().getFullName() + " has rescheduled your appointment"
                : "Your appointment has been rescheduled";

        record(Notification.RecipientType.PATIENT,
                String.valueOf(a.getPatient().getId()),
                a.getPatient().getEmail(), a.getPatient().getPhone(),
                Notification.Channel.EMAIL,
                Notification.Type.APPOINTMENT_RESCHEDULED + "_" + a.getRescheduleCount(),
                "Your MediBridge appointment has moved",
                """
                Hello %s,

                %s.

                New time: %s
                Previously: %s

                Nothing else changes - just press Join on MediBridge at the new
                time. The video room opens shortly before your slot.

                - MediBridge
                """.formatted(
                        a.getPatient().getFullName(),
                        who,
                        a.getAppointmentDate().format(WHEN),
                        a.getOriginalDate() == null ? "-" : a.getOriginalDate().format(WHEN)),
                "APPOINTMENT", String.valueOf(a.getId()));
    }

    public void sendHoldExpired(Appointment a) {
        deliver(a, Notification.Type.HOLD_EXPIRED,
                "Your MediBridge slot was released",
                """
                Hello %s,

                The slot with %s on %s was released because payment was not
                completed in time. The slot is now available to others.

                You are welcome to book again.

                - MediBridge
                """.formatted(
                        a.getPatient().getFullName(),
                        a.getDoctor().getFullName(),
                        a.getAppointmentDate().format(WHEN)));
    }

    public void sendPrescriptionReady(Appointment a) {
        deliver(a, Notification.Type.PRESCRIPTION_READY,
                "Your prescription from " + a.getDoctor().getFullName(),
                """
                Hello %s,

                %s has issued your prescription following the consultation on %s.

                You can view and download it as a PDF from Medical Records in
                the MediBridge app.

                - MediBridge
                """.formatted(
                        a.getPatient().getFullName(),
                        a.getDoctor().getFullName(),
                        a.getAppointmentDate().format(WHEN)));
    }

    // -------------------------------------------------------------- plumbing

    private void deliver(Appointment a, String type, String subject, String body) {
        record(Notification.RecipientType.PATIENT,
                String.valueOf(a.getPatient().getId()),
                a.getPatient().getEmail(), a.getPatient().getPhone(),
                Notification.Channel.EMAIL,
                type, subject, body,
                "APPOINTMENT", String.valueOf(a.getId()));
    }

    /**
     * Also sends a short WhatsApp/SMS to the patient's phone.
     *
     * <p>The channel copy is a separate notification row (with its own
     * {@code channel} in the de-dup key), so it neither duplicates nor blocks
     * the email. The body is deliberately terse — SMS caps at 160 characters.
     */
    private void deliverSms(Appointment a, String type, String shortBody) {
        Notification.Channel ch = smsService.isWhatsApp()
                ? Notification.Channel.WHATSAPP : Notification.Channel.SMS;
        record(Notification.RecipientType.PATIENT,
                String.valueOf(a.getPatient().getId()),
                a.getPatient().getEmail(), a.getPatient().getPhone(),
                ch, type, "(mobile)", shortBody,
                "APPOINTMENT", String.valueOf(a.getId()));
    }

    /**
     * @return false when this exact notification (per channel) was already sent -
     *         the caller does not need to care, but the reminder job relies on it.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean record(Notification.RecipientType recipientType, String recipientId,
                          String email, String phone, Notification.Channel channel,
                          String type, String subject, String body,
                          String entityType, String entityId) {

        if (repository.existsByTypeAndEntityTypeAndEntityIdAndRecipientIdAndChannel(
                type, entityType, entityId, recipientId, channel)) {
            log.debug("Skipping duplicate {} notification {} for {} {}",
                    channel, type, entityType, entityId);
            return false;
        }

        Notification notification = Notification.builder()
                .recipientType(recipientType)
                .recipientId(recipientId)
                .recipientEmail(email)   // the email column is always populated
                .channel(channel)
                .type(type)
                .subject(subject)
                .body(body)
                .status(Notification.Status.PENDING)
                .entityType(entityType)
                .entityId(entityId)
                .build();

        try {
            notification = repository.save(notification);
        } catch (DataIntegrityViolationException e) {
            // Another thread inserted the same notification between the check
            // above and this insert. The unique key did its job.
            log.debug("Duplicate notification blocked by constraint: {} {}", channel, type);
            return false;
        }

        try {
            switch (channel) {
                case EMAIL -> emailService.send(email, subject, body);
                case SMS, WHATSAPP -> smsService.send(phone, body);
                case PUSH -> { /* not implemented yet */ }
            }
            notification.setStatus(Notification.Status.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            notification.setStatus(Notification.Status.FAILED);
            notification.setErrorMessage(
                    e.getMessage() == null ? "unknown error"
                            : e.getMessage().substring(0, Math.min(240, e.getMessage().length())));
            log.error("{} notification {} failed: {}", channel, type, e.getMessage());
        }

        repository.save(notification);
        return true;
    }
}
