package com.medibridge.appointment;

import com.medibridge.admin.SettingsProvider;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Where a patient stands in their doctor's queue today, and how late that queue
 * is actually running.
 *
 * <p><b>The delay is observed, never self-reported.</b> {@code patient_joined_at}
 * and {@code doctor_joined_at} are stamped by the join endpoint - the only route
 * to the room URL - so they record when a consultation demonstrably began. The
 * gap between that and the slot time is real slippage. A doctor cannot declare
 * themselves on time, and nobody has to remember to update a status.
 *
 * <p><b>The estimate is a cascade, not a single delay applied to everyone.</b>
 * The day is walked in slot order carrying one value: the earliest moment the
 * doctor is next free. Each appointment starts at the later of its slot time and
 * that moment, so slippage propagates through a back-to-back run and is absorbed
 * by any gap in the schedule. The alternative - stamping one doctor-wide delay
 * on every remaining patient - keeps telling the 4pm patient about a 9am
 * overrun the doctor recovered from before lunch.
 *
 * <p>Three facts feed it, in descending order of how much they are trusted:
 * <ul>
 *   <li><b>An observed start.</b> Both parties in the room is the only moment a
 *       consultation can be said to have begun, so the start is
 *       {@code max(patientJoinedAt, doctorJoinedAt)}. Where it exists nothing is
 *       predicted - the time is simply read.</li>
 *   <li><b>The doctor is not free yet.</b> The consultation in progress pushes
 *       the next one out by the doctor's own consultation length.</li>
 *   <li><b>The slot time has passed and nobody opened the room.</b> Then it
 *       cannot start earlier than now, and the delay grows while the patient
 *       waits. Without this the estimate would freeze at the last consultation
 *       that finished, exactly when the patient is staring at it.</li>
 * </ul>
 *
 * <p>Nothing here is stored. A position or an ETA written to a row is wrong the
 * moment anyone books, cancels or joins - which on a busy day is constantly.
 *
 * <p>Only today's confirmed appointments get a queue. A position for next
 * Tuesday is a number nobody can act on.
 */
@Service
@RequiredArgsConstructor
public class LiveQueueService {

    /** Estimates are shown as "~25 min", so they are rounded to match. */
    private static final int ROUND_TO_MINUTES = 5;

    /** Used when a doctor has no consultation length on file. */
    private static final int DEFAULT_CONSULT_MINUTES = 30;

    private final AppointmentRepository appointmentRepository;
    private final SettingsProvider settings;

    /**
     * Injected rather than read from {@code LocalDateTime.now()} because every
     * number this class produces is measured from it - see {@code ClockConfig}.
     */
    private final Clock clock;

    /**
     * @param position     place in the doctor's queue today, 1 being the
     *                     consultation currently in the room
     * @param etaMinutes   minutes from now until this patient is likely to be
     *                     seen; 0 means they are being seen already
     * @param delayMinutes how much later than its slot time this consultation is
     *                     expected to start, 0 when it is on time
     */
    public record QueueView(Integer position, Integer etaMinutes, Integer delayMinutes) {
    }

    /**
     * Queue standing for a mixed bag of appointments - a patient's list spans
     * several doctors, most of it not for today.
     *
     * <p>Grouped by doctor so each doctor's day is read once rather than once
     * per appointment, and skipped entirely when nothing in the list is
     * queueable, which is the common case.
     */
    @Transactional(readOnly = true)
    public Map<Integer, QueueView> viewsFor(Collection<Appointment> appointments) {
        LocalDate today = LocalDate.now(clock);

        Set<String> doctorIds = new LinkedHashSet<>();
        for (Appointment a : appointments) {
            if (isQueued(a, today)) {
                doctorIds.add(a.getDoctor().getId());
            }
        }
        if (doctorIds.isEmpty()) {
            return Map.of();
        }

        Map<Integer, QueueView> views = new HashMap<>();
        for (String doctorId : doctorIds) {
            views.putAll(queueFor(doctorId, today));
        }
        return views;
    }

    /**
     * The whole of one doctor's queue for one day, keyed by appointment id.
     *
     * <p>Returned as a map of the entire day rather than one appointment at a
     * time: a position is only meaningful relative to the others, so computing
     * it per appointment would mean rebuilding the same day on every row.
     */
    @Transactional(readOnly = true)
    public Map<Integer, QueueView> queueFor(String doctorId, LocalDate day) {
        List<Appointment> booked = appointmentRepository.findDoctorDay(
                doctorId, day.atStartOfDay(), day.plusDays(1).atStartOfDay());

        LocalDateTime now = LocalDateTime.now(clock);
        Map<Integer, QueueView> views = new LinkedHashMap<>();

        // Nothing can start before now, so that is where the doctor's next free
        // moment begins. It only ever moves forward from here.
        LocalDateTime freeAt = now;
        int position = 0;

        // findDoctorDay orders by slot time, which is the order the doctor works
        // through them - so the running count is the queue position.
        for (Appointment a : booked) {
            if (a.getStatus() != AppointmentStatus.ACCEPTED) {
                // Completed, cancelled and no-show rows have left the queue, and
                // an unpaid hold never joined it. Skipping them here is what
                // makes positions shift down as the day is worked through.
                continue;
            }
            position++;

            LocalDateTime observed = observedStart(a);
            LocalDateTime start = observed != null
                    ? observed                             // read, not predicted
                    : later(a.getAppointmentDate(), freeAt);

            views.put(a.getId(), new QueueView(
                    position,
                    roundToStep(minutesBetween(now, start)),
                    delayMinutes(a.getAppointmentDate(), start)));

            freeAt = later(now, start.plusMinutes(consultMinutes(a)));
        }
        return views;
    }

    /**
     * When the consultation demonstrably began, or null if it has not.
     *
     * <p>Both parties must be in the room: a doctor waiting alone has not
     * started, and neither has a patient. Taking the later of the two joins is
     * what makes this the moment the consultation began rather than the moment
     * the first person showed up.
     *
     * <p>A patient's own lateness therefore delays the queue exactly as a
     * doctor's does. That is not a judgement about fault - it is what the next
     * patient's wait actually is, and the wait is what this number claims to
     * describe.
     */
    private LocalDateTime observedStart(Appointment a) {
        LocalDateTime patient = a.getPatientJoinedAt();
        LocalDateTime doctor = a.getDoctorJoinedAt();
        if (patient == null || doctor == null) {
            return null;
        }
        return later(patient, doctor);
    }

    /**
     * How late this consultation will start, rounded and capped.
     *
     * <p>Never negative: a doctor running ahead is not credit the next patient
     * can be shown, because their slot still will not open early.
     */
    private int delayMinutes(LocalDateTime scheduled, LocalDateTime start) {
        int late = minutesBetween(scheduled, start);

        // A room left open, or a no-show the sweep has not reached yet, would
        // otherwise report a delay measured in hours - which has stopped being
        // an estimate of a wait and become evidence that something is stuck.
        late = Math.min(late, settings.queueMaxDelayMinutes());

        // Below the floor there is no story to tell: a doctor three minutes
        // behind is on time, and saying otherwise invites the patient to treat
        // every estimate as noise.
        return late < settings.queueMinDelayMinutes() ? 0 : roundToStep(late);
    }

    /**
     * How long this doctor's consultations run.
     *
     * <p>Read from the doctor rather than from the booked slot: the slot is
     * lazily loaded and is detached outright when an appointment is cancelled,
     * so it is not reliably there to measure.
     */
    private int consultMinutes(Appointment a) {
        Integer minutes = a.getDoctor().getConsultationDurationMin();
        return minutes == null || minutes <= 0 ? DEFAULT_CONSULT_MINUTES : minutes;
    }

    /** Whole minutes from {@code from} to {@code to}, floored at zero. */
    private int minutesBetween(LocalDateTime from, LocalDateTime to) {
        return (int) Math.max(0, Duration.between(from, to).toMinutes());
    }

    /**
     * Rounds to the nearest {@value #ROUND_TO_MINUTES} minutes.
     *
     * <p>The figure is rendered as "~25 min", so minute precision would be a
     * claim the data does not support - and a value that reads 23, 24, 26 across
     * refreshes looks like a broken widget rather than an estimate.
     */
    private int roundToStep(int minutes) {
        return Math.round(minutes / (float) ROUND_TO_MINUTES) * ROUND_TO_MINUTES;
    }

    private LocalDateTime later(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private boolean isQueued(Appointment a, LocalDate today) {
        return a.getStatus() == AppointmentStatus.ACCEPTED
                && a.getAppointmentDate() != null
                && a.getAppointmentDate().toLocalDate().equals(today);
    }
}
