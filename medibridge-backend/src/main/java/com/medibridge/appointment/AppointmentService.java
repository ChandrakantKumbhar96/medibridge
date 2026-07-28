package com.medibridge.appointment;

import com.medibridge.admin.SettingsProvider;
import com.medibridge.appointment.dto.AppointmentResponse;
import com.medibridge.appointment.dto.BookAppointmentRequest;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AccountStatus;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ConflictException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.doctor.DoctorRepository;
import com.medibridge.doctor.DoctorScheduleRepository;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.notification.MeetingLinkService;
import com.medibridge.notification.NotificationService;
import com.medibridge.patient.PatientRepository;
import com.medibridge.patient.entity.Patient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Appointment lifecycle.
 *
 * <p><b>Booking model: slot-based confirmation.</b> A doctor who publishes a
 * slot has already agreed to be booked, so paying for that slot confirms it
 * outright - there is no separate accept/reject step. This mirrors Practo and
 * Apollo 24|7, and removes the window in which an already-paid patient could be
 * turned away. A doctor may still cancel, which refunds in full automatically.
 *
 * <p>States: {@code PENDING_PAYMENT -> ACCEPTED -> COMPLETED}, with
 * {@code CANCELLED} reachable from the first two and {@code AUTO_EXPIRED} set
 * by the hold-expiry job. {@code REQUESTED}/{@code RESCHEDULED} remain in the
 * enum for a future request-based flow but are not produced here.
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private static final List<AppointmentStatus> UPCOMING = List.of(
            AppointmentStatus.PENDING_PAYMENT, AppointmentStatus.REQUESTED,
            AppointmentStatus.ACCEPTED, AppointmentStatus.RESCHEDULED);

    private static final List<AppointmentStatus> PAST = List.of(
            AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED,
            AppointmentStatus.REJECTED, AppointmentStatus.AUTO_EXPIRED,
            AppointmentStatus.NO_SHOW);

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final MeetingLinkService meetingLinkService;
    private final NotificationService notificationService;
    private final SettingsProvider settings;
    private final ApplicationEventPublisher events;

    // -------------------------------------------------------------- booking

    /**
     * Books a slot and locks in the price.
     *
     * <p>Three layers guard against double booking: a pessimistic lock on the
     * slot row, an {@code isBooked} check inside that lock, and the UNIQUE
     * constraint on {@code appointment.schedule_id}. Only the last is a real
     * guarantee - the first two exist so the failure surfaces as a clean
     * "slot no longer available" rather than a driver-level error.
     *
     * <p>Fee and platform charge are snapshotted here. Reading them at payment
     * time would let a doctor's price change alter what an already-booked
     * patient owes.
     */
    @Transactional
    public AppointmentResponse book(Integer patientId, BookAppointmentRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (doctor.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("This doctor is not currently accepting appointments");
        }

        DoctorSchedule slot = scheduleRepository.findByIdForUpdate(request.scheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found"));

        if (!slot.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestException("That time slot belongs to a different doctor");
        }
        if (Boolean.TRUE.equals(slot.getIsBooked())
                || appointmentRepository.existsByScheduleId(slot.getId())) {
            throw new ConflictException("That time slot has just been taken. Please pick another.");
        }

        LocalDateTime when = LocalDateTime.of(slot.getAvailableDate(), slot.getStartTime());
        if (when.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("That time slot is in the past");
        }

        BigDecimal fee = doctor.getConsultationFee();
        BigDecimal platformFee = settings.platformFee();

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .schedule(slot)
                .appointmentDate(when)
                .status(AppointmentStatus.PENDING_PAYMENT)
                .consultType(request.consultType() == null ? "Consultation" : request.consultType())
                .reason(request.reason())
                .bookedFee(fee)
                .platformFee(platformFee)
                .totalAmount(fee.add(platformFee))
                .holdExpiresAt(LocalDateTime.now().plusMinutes(settings.slotHoldMinutes()))
                .build();

        try {
            appointment = appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("That time slot has just been taken. Please pick another.");
        }

        slot.setIsBooked(true);
        scheduleRepository.save(slot);

        notificationService.sendBookingPending(appointment);

        return AppointmentMapper.toDto(appointment, true);
    }

    /**
     * Payment succeeded: confirm the appointment and issue the meeting link.
     *
     * <p>The link is time-boxed. A URL that works forever is a URL anyone it was
     * forwarded to can walk into weeks later.
     */
    @Transactional
    public void markPaidAndConfirm(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.PENDING_PAYMENT) {
            return;   // already confirmed, or cancelled - nothing to do
        }

        appointment.setStatus(AppointmentStatus.ACCEPTED);
        appointment.setConfirmedAt(LocalDateTime.now());
        appointment.setHoldExpiresAt(null);

        appointment.setMeetingLink(meetingLinkService.generate());
        appointment.setMeetingSentAt(LocalDateTime.now());
        appointment.setMeetingJoinFrom(appointment.getAppointmentDate()
                .minusMinutes(settings.meetingJoinBeforeMinutes()));
        appointment.setMeetingValidUntil(appointment.getAppointmentDate()
                .plusMinutes(settings.meetingValidAfterMinutes()));

        appointmentRepository.save(appointment);
        notificationService.sendBookingConfirmed(appointment);
    }

    // -------------------------------------------------------- patient views

    @Transactional(readOnly = true)
    public Map<String, List<AppointmentResponse>> getPatientAppointments(Integer patientId) {
        List<AppointmentResponse> upcoming = appointmentRepository
                .findByPatientIdAndStatusInOrderByAppointmentDateAsc(patientId, UPCOMING)
                .stream().map(a -> AppointmentMapper.toDto(a, true)).toList();

        List<AppointmentResponse> past = appointmentRepository
                .findByPatientIdAndStatusInOrderByAppointmentDateDesc(patientId, PAST)
                .stream().map(a -> AppointmentMapper.toDto(a, false)).toList();

        Map<String, List<AppointmentResponse>> result = new LinkedHashMap<>();
        result.put("upcoming", upcoming);
        result.put("past", past);
        return result;
    }

    // --------------------------------------------------------- cancellation

    /**
     * Refund policy, applied automatically:
     * <ul>
     *   <li>doctor or admin cancels - always 100%, the patient did nothing wrong</li>
     *   <li>patient cancels with more than {@code free_cancellation_hours} to go - 100%</li>
     *   <li>patient cancels inside that window - {@code partial_refund_percent}%,
     *       because the slot is now unlikely to be resold</li>
     *   <li>patient cancels an appointment the <em>doctor</em> moved - always
     *       100%, whatever the clock says</li>
     * </ul>
     */
    @Transactional
    public AppointmentResponse cancelAsPatient(Integer patientId, Integer appointmentId,
                                               String reason) {
        Appointment appointment = appointmentRepository
                .findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        long hoursUntil = Duration.between(
                LocalDateTime.now(), appointment.getAppointmentDate()).toHours();

        // A doctor-initiated move leaves the patient holding a time they never
        // agreed to, often inside the penalty window. Billing them for that
        // would be charging them for the doctor's schedule change, so the
        // cutoff is waived for the rest of this booking's life.
        //
        // It is deliberately not time-boxed. reschedule() overwrites
        // rescheduled_by on every move, so the moment the patient reschedules
        // it themselves the flag flips back to PATIENT and the normal cutoff
        // returns - they chose the latest time, so they own it again.
        boolean movedByDoctor =
                appointment.getRescheduledBy() == Appointment.ActorRole.DOCTOR;

        int refundPercent =
                (movedByDoctor || hoursUntil >= settings.freeCancellationHours())
                        ? 100
                        : settings.partialRefundPercent();

        return cancel(appointment, Appointment.ActorRole.PATIENT, reason, refundPercent);
    }

    @Transactional
    public AppointmentResponse cancelAsDoctor(String doctorId, Integer appointmentId,
                                              String reason) {
        Appointment appointment = appointmentRepository
                .findByIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // The patient is not at fault, so they are always made whole.
        return cancel(appointment, Appointment.ActorRole.DOCTOR, reason, 100);
    }

    private AppointmentResponse cancel(Appointment appointment, Appointment.ActorRole by,
                                       String reason, int refundPercent) {
        if (!appointment.getStatus().isCancellable()) {
            throw new BadRequestException(
                    "This appointment can no longer be cancelled (status: "
                            + appointment.getStatus().getDbValue() + ")");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setCancelledBy(by);
        appointment.setCancellationReason(reason);

        // The link must die with the appointment.
        appointment.setMeetingValidUntil(LocalDateTime.now());

        appointmentRepository.save(appointment);
        releaseSlot(appointment);

        // The payment module refunds and notifies; it owns the money.
        events.publishEvent(new AppointmentCancelledEvent(
                appointment.getId(), refundPercent, reason));

        return AppointmentMapper.toDto(appointment, false);
    }

    /** Frees the slot so someone else can book it. */
    private void releaseSlot(Appointment appointment) {
        DoctorSchedule slot = appointment.getSchedule();
        if (slot != null) {
            slot.setIsBooked(false);
            scheduleRepository.save(slot);
            appointment.setSchedule(null);
            appointmentRepository.save(appointment);
        }
    }

    // ------------------------------------------------------------ reschedule

    /**
     * Moves a confirmed appointment to a different slot.
     *
     * <p>The appointment id is deliberately preserved. The payment, any
     * prescription and the audit trail all reference it, so cancelling and
     * rebooking would orphan them and force a refund/recharge cycle for what is
     * really just a change of time.
     *
     * <p>Both slot rows are locked before either is touched, always in ascending
     * id order. Two patients swapping into each other's slots concurrently would
     * otherwise be a textbook deadlock - each holding what the other needs.
     *
     * <p>The new slot is claimed before the old one is released. If that order
     * were reversed and the claim then failed, the patient would end up holding
     * neither.
     */
    @Transactional
    public AppointmentResponse reschedule(Integer appointmentId, Integer newScheduleId,
                                          Appointment.ActorRole by, Integer patientId,
                                          String doctorId) {

        Appointment appointment = (by == Appointment.ActorRole.PATIENT)
                ? appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"))
                : appointmentRepository.findByIdAndDoctorId(appointmentId, doctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.ACCEPTED) {
            throw new BadRequestException(
                    "Only a confirmed appointment can be rescheduled (status: "
                            + appointment.getStatus().getDbValue() + ")");
        }

        // Policy limits apply to patients only. A doctor with an emergency must
        // always be able to move a slot, and the patient is not penalised.
        if (by == Appointment.ActorRole.PATIENT) {
            int maxReschedules = settings.getInt("max_reschedules", 2);
            if (appointment.getRescheduleCount() != null
                    && appointment.getRescheduleCount() >= maxReschedules) {
                throw new BadRequestException(
                        "This appointment has already been rescheduled "
                                + maxReschedules + " times. Please cancel and book again.");
            }

            long hoursUntil = Duration.between(
                    LocalDateTime.now(), appointment.getAppointmentDate()).toHours();
            int minHours = settings.getInt("reschedule_min_hours", 4);
            if (hoursUntil < minHours) {
                throw new BadRequestException(
                        "Appointments can only be rescheduled at least " + minHours
                                + " hours in advance. Please cancel instead.");
            }
        }

        DoctorSchedule oldSlot = appointment.getSchedule();
        if (oldSlot != null && oldSlot.getId().equals(newScheduleId)) {
            throw new BadRequestException("That is already the booked slot");
        }

        // Lock in ascending id order to avoid a deadlock between two concurrent
        // reschedules moving in opposite directions.
        List<Integer> lockOrder = new ArrayList<>();
        lockOrder.add(newScheduleId);
        if (oldSlot != null) lockOrder.add(oldSlot.getId());
        Collections.sort(lockOrder);

        Map<Integer, DoctorSchedule> locked = new HashMap<>();
        for (Integer id : lockOrder) {
            locked.put(id, scheduleRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Time slot not found")));
        }

        DoctorSchedule newSlot = locked.get(newScheduleId);

        if (!newSlot.getDoctor().getId().equals(appointment.getDoctor().getId())) {
            throw new BadRequestException(
                    "The new slot belongs to a different doctor. "
                            + "To change doctor, cancel and book again.");
        }
        if (Boolean.TRUE.equals(newSlot.getIsBooked())
                || appointmentRepository.existsByScheduleId(newSlot.getId())) {
            throw new ConflictException("That time slot has just been taken. Please pick another.");
        }

        LocalDateTime newWhen = LocalDateTime.of(
                newSlot.getAvailableDate(), newSlot.getStartTime());
        if (newWhen.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("That time slot is in the past");
        }

        if (appointment.getOriginalDate() == null) {
            appointment.setOriginalDate(appointment.getAppointmentDate());
        }

        // Claim the new slot first; only then let the old one go.
        appointment.setSchedule(newSlot);
        appointment.setAppointmentDate(newWhen);
        appointment.setRescheduleCount(
                (short) ((appointment.getRescheduleCount() == null
                        ? 0 : appointment.getRescheduleCount()) + 1));
        appointment.setLastRescheduledAt(LocalDateTime.now());
        appointment.setRescheduledBy(by);

        // The join window moves with the appointment. The link itself is kept -
        // the patient already has it, and reissuing would invalidate a URL they
        // may have saved.
        appointment.setMeetingJoinFrom(
                newWhen.minusMinutes(settings.meetingJoinBeforeMinutes()));
        appointment.setMeetingValidUntil(
                newWhen.plusMinutes(settings.meetingValidAfterMinutes()));

        try {
            appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("That time slot has just been taken. Please pick another.");
        }

        newSlot.setIsBooked(true);
        scheduleRepository.save(newSlot);

        if (oldSlot != null) {
            DoctorSchedule released = locked.get(oldSlot.getId());
            released.setIsBooked(false);
            scheduleRepository.save(released);
        }

        notificationService.sendRescheduled(appointment, by.name());

        return AppointmentMapper.toDto(appointment, true);
    }

    // --------------------------------------------------------- doctor views

    @Transactional(readOnly = true)
    public Map<String, List<AppointmentResponse>> getDoctorDashboard(String doctorId) {
        LocalDate today = LocalDate.now();

        List<AppointmentResponse> todays = appointmentRepository
                .findDoctorDay(doctorId, today.atStartOfDay(), today.plusDays(1).atStartOfDay())
                .stream()
                .filter(a -> a.getStatus().isActive())
                .map(a -> AppointmentMapper.toDto(a, true))
                .toList();

        // Confirmed and still ahead - what the doctor needs to prepare for.
        List<AppointmentResponse> upcoming = appointmentRepository
                .findByDoctorIdAndStatusInOrderByAppointmentDateAsc(
                        doctorId, List.of(AppointmentStatus.ACCEPTED))
                .stream()
                .filter(a -> a.getAppointmentDate().isAfter(LocalDateTime.now()))
                .map(a -> AppointmentMapper.toDto(a, true))
                .toList();

        // Past their time but not yet written up - the doctor's actual to-do list.
        List<AppointmentResponse> awaitingNotes = appointmentRepository
                .findByDoctorIdAndStatusInOrderByAppointmentDateAsc(
                        doctorId, List.of(AppointmentStatus.ACCEPTED))
                .stream()
                .filter(Appointment::hasStarted)
                .map(a -> AppointmentMapper.toDto(a, true))
                .toList();

        // No-shows belong here too. A consultation the doctor turned up for and
        // the patient did not is still work they were paid for, and hiding it
        // would leave them unable to see why an unattended slot was billed.
        List<AppointmentResponse> completed = appointmentRepository
                .findByDoctorIdAndStatusInOrderByAppointmentDateAsc(
                        doctorId, List.of(AppointmentStatus.COMPLETED,
                                AppointmentStatus.NO_SHOW))
                .stream().map(a -> AppointmentMapper.toDto(a, false)).toList();

        Map<String, List<AppointmentResponse>> dashboard = new LinkedHashMap<>();
        dashboard.put("today", todays);
        dashboard.put("upcoming", upcoming);
        dashboard.put("pending", awaitingNotes);
        dashboard.put("completed", completed);
        return dashboard;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getDoctorAppointments(String doctorId) {
        return appointmentRepository.findAllForDoctor(doctorId).stream()
                .map(a -> AppointmentMapper.toDto(a, true))
                .toList();
    }

    /**
     * Doctor marks a consultation done.
     *
     * <p>Refuses to complete an appointment whose time has not arrived. Without
     * this a doctor could close - and prescribe for - a consultation scheduled
     * next month, which would then unlock a patient review for a visit that
     * never happened.
     */
    @Transactional
    public AppointmentResponse complete(String doctorId, Integer appointmentId) {
        Appointment appointment = appointmentRepository
                .findByIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.ACCEPTED) {
            throw new BadRequestException(
                    "Only a confirmed appointment can be completed (status: "
                            + appointment.getStatus().getDbValue() + ")");
        }
        if (!appointment.hasStarted()) {
            throw new BadRequestException(
                    "This consultation is scheduled for "
                            + AppointmentMapper.describe(appointment)
                            + " and cannot be completed before then");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        // The payout module listens and accrues the doctor's earning. Announced
        // rather than called directly so this module stays unaware of money.
        events.publishEvent(new AppointmentCompletedEvent(appointment.getId()));

        return AppointmentMapper.toDto(appointment, false);
    }

    // ------------------------------------------------------- video join

    private static final java.time.format.DateTimeFormatter JOIN_TIME =
            java.time.format.DateTimeFormatter.ofPattern("EEE d MMM, hh:mm a", Locale.ENGLISH);

    /**
     * Returns the consultation room URL, minted on demand.
     *
     * <p>This is the industry-correct flow: the link is a bearer secret, so it is
     * never emailed or embedded in list responses. The patient (or treating
     * doctor) asks for it at join time, and the server hands it over only if the
     * caller owns the appointment, it is paid and confirmed, and the current
     * time is inside the join window.
     */
    @Transactional
    public String getJoinLinkAsPatient(Integer patientId, Integer appointmentId) {
        Appointment a = appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        String link = resolveJoinLink(a);
        stampAttendance(a, Appointment.ActorRole.PATIENT);
        return link;
    }

    @Transactional
    public String getJoinLinkAsDoctor(String doctorId, Integer appointmentId) {
        Appointment a = appointmentRepository.findByIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        String link = resolveJoinLink(a);
        stampAttendance(a, Appointment.ActorRole.DOCTOR);
        return link;
    }

    /**
     * Records the first time a party opened the room, and only the first.
     *
     * <p>Reconnecting after a dropped call must not read as a later arrival, and
     * the no-show question is only ever "did they come at all". Stamped after
     * {@link #resolveJoinLink} succeeds, so a rejected request - wrong window,
     * wrong owner, unpaid - never counts as attendance.
     */
    private void stampAttendance(Appointment a, Appointment.ActorRole who) {
        if (who == Appointment.ActorRole.PATIENT && a.getPatientJoinedAt() == null) {
            a.setPatientJoinedAt(LocalDateTime.now());
        } else if (who == Appointment.ActorRole.DOCTOR && a.getDoctorJoinedAt() == null) {
            a.setDoctorJoinedAt(LocalDateTime.now());
        } else {
            return;
        }
        appointmentRepository.save(a);
    }

    private String resolveJoinLink(Appointment a) {
        if (a.getStatus() != AppointmentStatus.ACCEPTED) {
            throw new BadRequestException(
                    "This consultation is not open to join (status: "
                            + a.getStatus().getDbValue() + ").");
        }
        if (a.getMeetingLink() == null) {
            throw new BadRequestException("No consultation room is set for this appointment.");
        }
        if (!a.isMeetingJoinable()) {
            LocalDateTime from = a.getMeetingJoinFrom();
            throw new BadRequestException(from == null
                    ? "The consultation room is not open yet."
                    : "The consultation room opens on " + from.format(JOIN_TIME)
                            + ". Please come back then.");
        }
        return a.getMeetingLink();
    }

    // ------------------------------------------------------- scheduled work

    /**
     * Releases slots whose payment hold ran out.
     *
     * <p>Called by {@code AppointmentScheduler}. Each appointment is handled in
     * its own transaction so one bad row cannot block the rest.
     */
    @Transactional
    public int expireHold(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null || !appointment.isHoldExpired()) {
            return 0;
        }

        appointment.setStatus(AppointmentStatus.AUTO_EXPIRED);
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setCancelledBy(Appointment.ActorRole.SYSTEM);
        appointment.setCancellationReason("Payment not completed within the hold period");
        appointmentRepository.save(appointment);

        releaseSlot(appointment);
        notificationService.sendHoldExpired(appointment);

        log.info("Released slot for expired hold on appointment {}", appointmentId);
        return 1;
    }

    @Transactional(readOnly = true)
    public List<Integer> findExpiredHoldIds() {
        return appointmentRepository
                .findByStatusAndHoldExpiresAtBefore(
                        AppointmentStatus.PENDING_PAYMENT, LocalDateTime.now())
                .stream().map(Appointment::getId).toList();
    }

    // ------------------------------------------------------------- no-show

    /**
     * Appointments whose room has closed with at least one party never having
     * opened it.
     *
     * <p>The grace period is measured from {@code meeting_valid_until}, not from
     * the slot time: the room deliberately stays open past the end, and someone
     * who joins late still attended.
     */
    @Transactional(readOnly = true)
    public List<Integer> findNoShowCandidateIds() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusMinutes(settings.getInt("no_show_grace_minutes", 15));
        return appointmentRepository.findNoShowCandidateIds(AppointmentStatus.ACCEPTED, cutoff);
    }

    /**
     * Closes out an appointment nobody attended, and settles the money.
     *
     * <p>The patient forfeits only where the doctor demonstrably turned up.
     * Every other case refunds in full - if the platform cannot show the doctor
     * was there, it cannot bill the patient for the empty room. That asymmetry
     * is deliberate: it prices the platform's own failures, not the patient's.
     *
     * <p>A patient no-show pays the doctor exactly as a completed consultation
     * would. They held the hour; the patient's absence is not their loss to
     * carry.
     *
     * @return 1 if the appointment was closed as a no-show, 0 if it was left
     *         alone (already settled, or both parties actually attended)
     */
    @Transactional
    public int settleNoShow(Integer appointmentId) {
        Appointment a = appointmentRepository.findById(appointmentId).orElse(null);
        if (a == null || a.getStatus() != AppointmentStatus.ACCEPTED) {
            return 0;
        }

        Appointment.NoShowParty party = a.decideNoShowParty();
        if (party == null) {
            // Both joined. This is a real consultation waiting on notes, and it
            // belongs to the doctor's to-do list - not to this sweep.
            return 0;
        }

        int refundPercent = party == Appointment.NoShowParty.PATIENT ? 0 : 100;

        a.setStatus(AppointmentStatus.NO_SHOW);
        a.setNoShowBy(party);
        a.setNoShowMarkedAt(LocalDateTime.now());
        appointmentRepository.save(a);

        if (refundPercent == 0) {
            // Reuses the completion event on purpose: the payout module's job is
            // to accrue what the doctor earned, and they earned this.
            events.publishEvent(new AppointmentCompletedEvent(a.getId()));

            // No money moves, so nothing downstream would tell the patient. They
            // still need to know the slot was consumed and why.
            notificationService.sendNoShow(a, party.name(), java.math.BigDecimal.ZERO);
        } else {
            events.publishEvent(new AppointmentNoShowEvent(
                    a.getId(), refundPercent, party.name()));
        }

        log.info("Appointment {} closed as no-show by {} ({}% refund)",
                appointmentId, party, refundPercent);
        return 1;
    }

    @Transactional(readOnly = true)
    public List<Appointment> findAppointmentsNeedingReminder() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusHours(settings.reminderHoursBefore());
        return appointmentRepository.findByStatusAndAppointmentDateBetween(
                AppointmentStatus.ACCEPTED, from, to);
    }

    @Transactional(readOnly = true)
    public Optional<Appointment> findById(Integer id) {
        return appointmentRepository.findById(id);
    }

    /** Published on cancellation so the payment module can refund. */
    public record AppointmentCancelledEvent(Integer appointmentId, int refundPercent,
                                            String reason) {
    }

    /** Published on completion so the payout module can accrue the earning. */
    public record AppointmentCompletedEvent(Integer appointmentId) {
    }

    /**
     * Published when an appointment is written off as unattended and money is
     * owed back. Kept separate from {@link AppointmentCancelledEvent} because
     * the patient must not be told their appointment was "cancelled" - nobody
     * cancelled it, and the wording decides whether they trust the refund.
     *
     * @param noShowBy PATIENT, DOCTOR or BOTH
     */
    public record AppointmentNoShowEvent(Integer appointmentId, int refundPercent,
                                         String noShowBy) {
    }
}
