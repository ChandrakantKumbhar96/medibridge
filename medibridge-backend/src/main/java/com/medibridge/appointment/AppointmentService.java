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
            AppointmentStatus.REJECTED, AppointmentStatus.AUTO_EXPIRED);

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

        int refundPercent = hoursUntil >= settings.freeCancellationHours()
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

        List<AppointmentResponse> completed = appointmentRepository
                .findByDoctorIdAndStatusInOrderByAppointmentDateAsc(
                        doctorId, List.of(AppointmentStatus.COMPLETED))
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

        return AppointmentMapper.toDto(appointment, false);
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
}
