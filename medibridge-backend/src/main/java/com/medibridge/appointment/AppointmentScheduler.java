package com.medibridge.appointment;

import com.medibridge.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Background jobs that keep appointment state honest.
 *
 * <p>Each item is processed in its own transaction (see
 * {@code AppointmentService.expireHold}) so a single bad row cannot stall the
 * whole sweep.
 *
 * <p>Note for multi-instance deployment: these would need a shared lock
 * (ShedLock or similar) so two servers do not run the same sweep concurrently.
 * The reminder de-duplication key makes double-sending harmless in the
 * meantime, which is why that constraint exists.
 */
@Component
@RequiredArgsConstructor
public class AppointmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentScheduler.class);

    private final AppointmentService appointmentService;
    private final NotificationService notificationService;

    /**
     * Releases slots held by abandoned checkouts.
     *
     * <p>Runs every minute: the hold is measured in minutes, so a slower sweep
     * would leave slots blocked well past their expiry.
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void releaseExpiredHolds() {
        List<Integer> expired = appointmentService.findExpiredHoldIds();
        if (expired.isEmpty()) {
            return;
        }

        int released = 0;
        for (Integer id : expired) {
            try {
                released += appointmentService.expireHold(id);
            } catch (Exception e) {
                log.error("Could not expire hold on appointment {}: {}", id, e.getMessage());
            }
        }
        log.info("Hold sweep: released {} of {} expired holds", released, expired.size());
    }

    /**
     * Sends the pre-appointment reminder.
     *
     * <p>Hourly is enough for a 24-hour reminder, and the notification unique
     * key means re-running never double-sends.
     */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    public void sendUpcomingReminders() {
        var due = appointmentService.findAppointmentsNeedingReminder();
        if (due.isEmpty()) {
            return;
        }

        int sent = 0;
        for (var appointment : due) {
            try {
                notificationService.sendReminder(appointment);
                sent++;
            } catch (Exception e) {
                log.error("Reminder failed for appointment {}: {}",
                        appointment.getId(), e.getMessage());
            }
        }
        log.info("Reminder sweep: processed {} upcoming appointments", sent);
    }
}
