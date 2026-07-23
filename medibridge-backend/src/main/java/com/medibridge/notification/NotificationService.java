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

                Join here at your appointment time:
                %s

                The link opens in your browser - no account or installation
                needed. It becomes active shortly before your appointment.

                - MediBridge
                """.formatted(
                        a.getPatient().getFullName(),
                        a.getDoctor().getFullName(),
                        a.getAppointmentDate().format(WHEN),
                        a.getTotalAmount(),
                        a.getMeetingLink()));
    }

    public void sendReminder(Appointment a) {
        deliver(a, Notification.Type.REMINDER_24H,
                "Reminder: your MediBridge consultation is tomorrow",
                """
                Hello %s,

                A reminder that your consultation with %s is on %s.

                Join here:
                %s

                If you can no longer attend, please cancel from the app so the
                slot can be offered to someone else.

                - MediBridge
                """.formatted(
                        a.getPatient().getFullName(),
                        a.getDoctor().getFullName(),
                        a.getAppointmentDate().format(WHEN),
                        a.getMeetingLink() == null ? "(available after confirmation)"
                                : a.getMeetingLink()));
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
                a.getPatient().getEmail(),
                Notification.Type.APPOINTMENT_RESCHEDULED + "_" + a.getRescheduleCount(),
                "Your MediBridge appointment has moved",
                """
                Hello %s,

                %s.

                New time: %s
                Previously: %s

                Your existing consultation link still works:
                %s

                - MediBridge
                """.formatted(
                        a.getPatient().getFullName(),
                        who,
                        a.getAppointmentDate().format(WHEN),
                        a.getOriginalDate() == null ? "-" : a.getOriginalDate().format(WHEN),
                        a.getMeetingLink() == null ? "(available after confirmation)"
                                : a.getMeetingLink()),
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
                a.getPatient().getEmail(),
                type, subject, body,
                "APPOINTMENT", String.valueOf(a.getId()));
    }

    /**
     * @return false when this exact notification was already sent - the caller
     *         does not need to care, but the reminder job relies on it.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean record(Notification.RecipientType recipientType, String recipientId,
                          String email, String type, String subject, String body,
                          String entityType, String entityId) {

        if (repository.existsByTypeAndEntityTypeAndEntityIdAndRecipientId(
                type, entityType, entityId, recipientId)) {
            log.debug("Skipping duplicate notification {} for {} {}", type, entityType, entityId);
            return false;
        }

        Notification notification = Notification.builder()
                .recipientType(recipientType)
                .recipientId(recipientId)
                .recipientEmail(email)
                .channel(Notification.Channel.EMAIL)
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
            log.debug("Duplicate notification blocked by constraint: {}", type);
            return false;
        }

        try {
            emailService.send(email, subject, body);
            notification.setStatus(Notification.Status.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            notification.setStatus(Notification.Status.FAILED);
            notification.setErrorMessage(
                    e.getMessage() == null ? "unknown error"
                            : e.getMessage().substring(0, Math.min(240, e.getMessage().length())));
            log.error("Notification {} failed: {}", type, e.getMessage());
        }

        repository.save(notification);
        return true;
    }
}
