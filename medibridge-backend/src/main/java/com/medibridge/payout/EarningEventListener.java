package com.medibridge.payout;

import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.appointment.AppointmentService;
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
     * <p>Without this, cancelling a completed-and-refunded appointment would
     * leave the platform paying a doctor for money it had given back.
     */
    @TransactionalEventListener
    public void onCancelled(AppointmentService.AppointmentCancelledEvent event) {
        try {
            payoutService.reverseEarning(event.appointmentId(), event.reason());
        } catch (Exception e) {
            log.error("Could not reverse earning for appointment {}: {}",
                    event.appointmentId(), e.getMessage());
        }
    }
}
