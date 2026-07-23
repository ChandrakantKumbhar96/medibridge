package com.medibridge.payout;

import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.appointment.AppointmentService;
import com.medibridge.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps the doctor ledger in step with appointment outcomes.
 *
 * <p>AFTER_COMMIT on both handlers: accruing an earning for a completion that
 * then rolls back would credit a doctor for a consultation the database never
 * recorded.
 */
@Component
@RequiredArgsConstructor
public class EarningEventListener {

    private static final Logger log = LoggerFactory.getLogger(EarningEventListener.class);

    private final PayoutService payoutService;
    private final AppointmentRepository appointmentRepository;

    @TransactionalEventListener
    public void onCompleted(AppointmentService.AppointmentCompletedEvent event) {
        appointmentRepository.findById(event.appointmentId()).ifPresent(appointment -> {
            try {
                payoutService.recordEarning(appointment);
            } catch (Exception e) {
                // The consultation is complete and must stay complete. A missing
                // ledger row is recoverable; losing the clinical record is not.
                log.error("Could not record earning for appointment {} - "
                        + "needs manual reconciliation: {}",
                        event.appointmentId(), e.getMessage());
            }
        });
    }

    /**
     * A refunded consultation is no longer owed to the doctor.
     *
     * <p>Driven by the payment module rather than the cancellation path,
     * because there are two ways money goes back: a cancellation, and an admin
     * issuing a manual refund on a completed consultation. Listening only to
     * cancellations left the second case refunding the patient while still
     * paying the doctor.
     */
    @TransactionalEventListener
    public void onRefunded(PaymentService.PaymentRefundedEvent event) {
        try {
            payoutService.reverseEarning(event.appointmentId(), event.reason());
        } catch (Exception e) {
            log.error("Could not reverse earning for appointment {} after refund: {}",
                    event.appointmentId(), e.getMessage());
        }
    }
}
