package com.medibridge.payment;

import com.medibridge.appointment.AppointmentService;
import com.medibridge.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

/**
 * Refunds automatically when an appointment is cancelled.
 *
 * <p>Wired as an event listener rather than a direct call so the appointment
 * module never has to know about payments - it announces what happened, and the
 * module that owns the money decides what that costs.
 *
 * <p>{@code @TransactionalEventListener} (AFTER_COMMIT by default) matters
 * here: refunding before the cancellation is committed could return money for a
 * cancellation that then rolls back.
 */
@Component
@RequiredArgsConstructor
public class RefundOnCancellationListener {

    private static final Logger log = LoggerFactory.getLogger(RefundOnCancellationListener.class);

    private final PaymentService paymentService;
    private final AppointmentService appointmentService;
    private final NotificationService notificationService;

    @TransactionalEventListener
    public void onAppointmentCancelled(AppointmentService.AppointmentCancelledEvent event) {
        BigDecimal refunded = BigDecimal.ZERO;

        try {
            refunded = paymentService.refundForCancellation(
                    event.appointmentId(), event.refundPercent(), event.reason());

            if (refunded.signum() > 0) {
                log.info("Refunded Rs.{} ({}%) for cancelled appointment {}",
                        refunded, event.refundPercent(), event.appointmentId());
            }
        } catch (Exception e) {
            // The cancellation itself already committed and must stand. A failed
            // refund becomes an admin task, not a reason to un-cancel.
            log.error("Automatic refund failed for appointment {} - needs manual review: {}",
                    event.appointmentId(), e.getMessage());
        }

        final BigDecimal amount = refunded;
        appointmentService.findById(event.appointmentId()).ifPresent(appointment ->
                notificationService.sendCancelled(appointment, amount, event.reason()));
    }
}
