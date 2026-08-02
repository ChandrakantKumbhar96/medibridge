package com.medibridge.appointment;

import com.medibridge.admin.SettingsProvider;
import com.medibridge.appointment.dto.AppointmentResponse;
import com.medibridge.appointment.dto.BookAppointmentRequest;
import com.medibridge.appointment.dto.NextAvailableResponse;
import com.medibridge.appointment.dto.RoomStatusResponse;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AccountStatus;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.common.enums.ConsultType;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ConflictException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.doctor.DoctorRepository;
import com.medibridge.doctor.DoctorScheduleRepository;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.notification.MeetingLinkService;
import com.medibridge.notification.NotificationService;
import com.medibridge.patient.FamilyMemberService;
import com.medibridge.patient.PatientRepository;
import com.medibridge.patient.entity.FamilyMember;
import com.medibridge.patient.entity.Patient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    // Same formatting as AppointmentMapper, kept local rather than shared:
    // that class's formatters are private, and duplicating two constants beats
    // widening its API for one caller.
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH);

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final com.medibridge.doctor.SpecializationRepository specializationRepository;
    private final MeetingLinkService meetingLinkService;
    private final NotificationService notificationService;
    private final SettingsProvider settings;
    private final LiveQueueService liveQueue;
    // Another module's Service, never its repository - the appointment module
    // must not know how documents are stored, only how many there are.
    private final com.medibridge.record.RecordService recordService;
    // Same rule: the dependent is resolved through the patient module's service,
    // which is where "is this yours" is answered.
    private final FamilyMemberService familyMemberService;
    private final ApplicationEventPublisher events;

    // -------------------------------------------------------------- booking

    /**
     * The soonest open slot in the configured window, optionally narrowed to
     * one specialization - for a patient who would rather be matched than
     * browse. Takes the specialization by name, not id: that is what the
     * specialty chips on FindDoctors already hold, the same as {@code
     * DoctorService.listDoctors}.
     *
     * <p>Read-only: nothing is held or reserved. {@code scheduleId} is handed
     * back into {@link #book} to confirm, so a slot taken between preview and
     * confirm surfaces as the same "just been taken" conflict {@code book}
     * already throws - no separate race handling needed here.
     *
     * <p>The floor keeps the offer reachable; without it a slot starting in two
     * minutes could be shown to a patient who has not paid yet.
     */
    @Transactional(readOnly = true)
    public Optional<NextAvailableResponse> nextAvailable(String specialization) {
        Integer specializationId = null;
        if (specialization != null && !specialization.isBlank() && !"All".equalsIgnoreCase(specialization)) {
            specializationId = specializationRepository.findByNameIgnoreCase(specialization)
                    .map(com.medibridge.doctor.entity.Specialization::getId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Unknown specialization: " + specialization));
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusMinutes(settings.nextAvailableFloorMinutes());
        LocalDateTime until = now.plusHours(settings.nextAvailableWindowHours());

        List<DoctorSchedule> matches = scheduleRepository.findEarliestOpenSlots(
                specializationId, from, until, PageRequest.of(0, 1));
        if (matches.isEmpty()) {
            return Optional.empty();
        }

        DoctorSchedule slot = matches.get(0);
        Doctor doctor = slot.getDoctor();
        LocalDateTime slotStart = LocalDateTime.of(slot.getAvailableDate(), slot.getStartTime());

        return Optional.of(new NextAvailableResponse(
                doctor.getId(),
                doctor.getFullName(),
                doctor.getSpecialization().getName(),
                slot.getId(),
                slot.getAvailableDate().format(DATE_FORMAT),
                slotStart.format(TIME_FORMAT).toUpperCase(java.util.Locale.ENGLISH),
                doctor.getConsultationFee(),
                (int) Math.max(0, Duration.between(now, slotStart).toMinutes())));
    }

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

        // Null for the ordinary "booking for myself" case. Anything else is
        // verified to belong to this caller before it is used, and a dependent
        // owned by someone else comes back as 404 - never 403, which would
        // confirm the id exists.
        FamilyMember bookedFor =
                familyMemberService.requireOwned(patientId, request.familyMemberId());

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

        String consultType = request.consultType() == null
                ? ConsultType.CONSULTATION : request.consultType();

        // A second opinion is a review of an existing case. With nothing on file
        // to review, the specialist has nothing to form an opinion on - and the
        // patient pays a premium for a consultation that cannot deliver what the
        // page promised them.
        boolean secondOpinion = ConsultType.isSecondOpinion(consultType);
        if (secondOpinion) {
            int required = settings.secondOpinionMinReports();
            // Counted for whoever the visit is for, not for the account holder.
            // A parent with ten of their own reports still has nothing for a
            // specialist to review about their child.
            Integer subjectId = bookedFor == null ? null : bookedFor.getId();
            if (required > 0 && recordService.countForSubject(patientId, subjectId) < required) {
                throw new BadRequestException(
                        "Please upload at least " + required + " report"
                                + (required == 1 ? "" : "s")
                                + " for " + (bookedFor == null ? "yourself" : bookedFor.getFullName())
                                + " before requesting a second opinion - the specialist "
                                + "needs their existing reports to review.");
            }
        }

        // Reviewing another clinician's case file is more work than a first
        // consultation, so it is not billed at the same rate. Applied here, at
        // booking, because this is where the price is snapshotted - reading the
        // multiplier at payment time would let it change under a booked patient.
        BigDecimal fee = secondOpinion
                ? premiumFee(doctor.getConsultationFee())
                : doctor.getConsultationFee();
        BigDecimal platformFee = settings.platformFee();

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .familyMember(bookedFor)
                .doctor(doctor)
                .schedule(slot)
                .appointmentDate(when)
                .status(AppointmentStatus.PENDING_PAYMENT)
                .consultType(consultType)
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
     * The doctor's fee scaled by the second-opinion percentage.
     *
     * <p>HALF_UP to two places so the stored amount is a real payable sum - the
     * gateway is charged this exact figure, and an unrounded value would make
     * the invoice disagree with what was taken.
     */
    private BigDecimal premiumFee(BigDecimal baseFee) {
        return baseFee
                .multiply(BigDecimal.valueOf(settings.secondOpinionFeePercent()))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
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

        confirm(appointment);
    }

    /**
     * Everything that makes a booking real: confirmed status, a time-boxed room,
     * and the patient told.
     *
     * <p>Extracted so the free follow-up - which has no payment to wait on -
     * lands in exactly the state a paid booking reaches. Two copies of this
     * would drift, and the one that drifts is the one that issues no meeting
     * link.
     */
    private void confirm(Appointment appointment) {
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

    // ---------------------------------------------------------- follow-up

    /**
     * Books the one free revisit a completed consultation earns.
     *
     * <p>A patient who was seen last Tuesday and needs five minutes to report
     * back should not pay a second full fee for it - clinics have always worked
     * this way, and charging for it is what pushes people into not following up
     * at all. The window runs from {@code completed_at} for
     * {@code follow_up_window_days}, and the revisit must be with the same
     * doctor: a different clinician has not seen the case, so there is nothing
     * to follow up on.
     *
     * <p><b>One per parent, and the UNIQUE index is what says so.</b> The
     * {@code existsBy} check below only exists so the ordinary case reads as
     * "already used" rather than a constraint violation; two concurrent requests
     * both passing it is expected, and the loser is caught at the insert as a
     * 409. A boolean on the parent set from Java would be the same mistake as
     * treating {@code doctor_schedule.is_booked} as the booking guard.
     */
    @Transactional
    public AppointmentResponse bookFollowUp(Integer patientId, Integer parentId,
                                            Integer scheduleId) {
        if (!settings.followUpEnabled()) {
            throw new BadRequestException("Free follow-ups are not currently available.");
        }

        // Ownership is the (id, ownerId) lookup, and a miss is a 404 - telling a
        // stranger that this appointment exists is itself a disclosure.
        Appointment parent = appointmentRepository.findByIdAndPatientId(parentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (parent.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BadRequestException(
                    "Only a completed consultation earns a free follow-up (status: "
                            + parent.getStatus().getDbValue() + ")");
        }
        if (parent.isFollowUp()) {
            throw new BadRequestException(
                    "This is already a follow-up. A free revisit is earned by a paid "
                            + "consultation, not by another follow-up.");
        }

        int windowDays = settings.followUpWindowDays();
        LocalDateTime eligibleUntil = parent.followUpEligibleUntil(windowDays);
        if (eligibleUntil == null || eligibleUntil.isBefore(LocalDateTime.now())) {
            throw new BadRequestException(
                    "The free follow-up window for this consultation closed after "
                            + windowDays + " days. Please book a new appointment.");
        }
        if (appointmentRepository.existsByParentAppointmentId(parentId)) {
            throw new ConflictException(
                    "You have already used the free follow-up for this consultation.");
        }

        DoctorSchedule slot = scheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found"));

        if (!slot.getDoctor().getId().equals(parent.getDoctor().getId())) {
            throw new BadRequestException(
                    "A follow-up must be with the same doctor who saw you.");
        }
        if (Boolean.TRUE.equals(slot.getIsBooked())
                || appointmentRepository.existsByScheduleId(slot.getId())) {
            throw new ConflictException("That time slot has just been taken. Please pick another.");
        }

        LocalDateTime when = LocalDateTime.of(slot.getAvailableDate(), slot.getStartTime());
        if (when.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("That time slot is in the past");
        }

        Appointment followUp = Appointment.builder()
                .patient(parent.getPatient())
                // The revisit is for whoever was seen, not for whoever paid - a
                // parent's follow-up on their child's consultation is still the
                // child's appointment.
                .familyMember(parent.getFamilyMember())
                .doctor(parent.getDoctor())
                .schedule(slot)
                .parentAppointment(parent)
                .appointmentDate(when)
                .status(AppointmentStatus.PENDING_PAYMENT)
                .consultType(ConsultType.FOLLOW_UP)
                .reason(parent.getReason())
                // Free means free: a platform fee on a zero-fee revisit is still
                // a charge the patient was told they would not pay.
                .bookedFee(BigDecimal.ZERO)
                .platformFee(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        try {
            followUp = appointmentRepository.saveAndFlush(followUp);
        } catch (DataIntegrityViolationException e) {
            // Either uq_follow_up_parent or uq_schedule_booking. Both are races
            // this request lost, and both are a 409 rather than a 500.
            throw new ConflictException(isParentClash(e)
                    ? "You have already used the free follow-up for this consultation."
                    : "That time slot has just been taken. Please pick another.");
        }

        slot.setIsBooked(true);
        scheduleRepository.save(slot);

        // Nothing to pay, so nothing confirms it later. It reaches the same
        // state a paid booking does, here.
        confirm(followUp);

        // The zero fee is announced rather than enacted: this module decides the
        // price is nil, the payment module decides what that means for invoices
        // and payouts. Same split as the refund percentage on cancellation.
        events.publishEvent(new FollowUpBookedEvent(
                followUp.getId(), parent.getId(), followUp.getTotalAmount()));

        return AppointmentMapper.toDto(followUp, true);
    }

    /**
     * True when the failed insert was a second follow-up off one parent rather
     * than a lost race for the slot.
     *
     * <p>Matched on the constraint name because the two failures need different
     * wording - "pick another time" is useless advice to someone whose free
     * revisit is simply gone. Falls back to the slot message, which is by far
     * the commoner clash.
     */
    private boolean isParentClash(DataIntegrityViolationException e) {
        String detail = String.valueOf(e.getMostSpecificCause().getMessage());
        return detail.contains("uq_follow_up_parent");
    }

    // -------------------------------------------------------- patient views

    @Transactional(readOnly = true)
    public Map<String, List<AppointmentResponse>> getPatientAppointments(Integer patientId) {
        List<Appointment> upcomingRows = appointmentRepository
                .findByPatientIdAndStatusInOrderByAppointmentDateAsc(patientId, UPCOMING);

        // Only today's confirmed bookings come back with a standing; the rest of
        // the list maps to null and simply has no queue fields.
        Map<Integer, LiveQueueService.QueueView> queue = liveQueue.viewsFor(upcomingRows);

        List<AppointmentResponse> upcoming = upcomingRows.stream()
                .map(a -> AppointmentMapper.toDto(a, true, queue.get(a.getId())))
                .toList();

        // Whether each completed consultation still has its free revisit going,
        // resolved once for the whole list rather than per row.
        boolean followUps = settings.followUpEnabled();
        int windowDays = settings.followUpWindowDays();
        Set<Integer> spent = followUps
                ? new HashSet<>(appointmentRepository.findUsedFollowUpParentIds(patientId))
                : Set.of();
        LocalDateTime now = LocalDateTime.now();

        List<AppointmentResponse> past = appointmentRepository
                .findByPatientIdAndStatusInOrderByAppointmentDateDesc(patientId, PAST)
                .stream()
                .map(a -> AppointmentMapper.toDto(a, false, null,
                        followUpEligibleUntil(a, followUps, windowDays, spent, now)))
                .toList();

        Map<String, List<AppointmentResponse>> result = new LinkedHashMap<>();
        result.put("upcoming", upcoming);
        result.put("past", past);
        return result;
    }

    /**
     * The deadline to send for one past row, or null if there is nothing to
     * offer.
     *
     * <p>Null covers every reason at once - feature off, not a completed visit,
     * window closed, revisit already taken, or this row is itself a follow-up -
     * because NON_NULL then drops the field and the UI's only rule is "present
     * means offer it". Encoding the reasons would put this policy in two places.
     */
    private LocalDateTime followUpEligibleUntil(Appointment a, boolean enabled,
                                                int windowDays, Set<Integer> spent,
                                                LocalDateTime now) {
        if (!enabled || a.isFollowUp() || spent.contains(a.getId())) {
            return null;
        }
        LocalDateTime until = a.followUpEligibleUntil(windowDays);
        return until != null && until.isAfter(now) ? until : null;
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
            int maxReschedules = settings.maxReschedules();
            if (appointment.getRescheduleCount() != null
                    && appointment.getRescheduleCount() >= maxReschedules) {
                throw new BadRequestException(
                        "This appointment has already been rescheduled "
                                + maxReschedules + " times. Please cancel and book again.");
            }

            long hoursUntil = Duration.between(
                    LocalDateTime.now(), appointment.getAppointmentDate()).toHours();
            int minHours = settings.rescheduleMinHours();
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

        // The doctor sees the same queue their patients do - same positions,
        // same running-late figure. Two versions of "how late am I" would be one
        // version too many.
        Map<Integer, LiveQueueService.QueueView> queue = liveQueue.queueFor(doctorId, today);

        List<AppointmentResponse> todays = appointmentRepository
                .findDoctorDay(doctorId, today.atStartOfDay(), today.plusDays(1).atStartOfDay())
                .stream()
                .filter(a -> a.getStatus().isActive())
                .map(a -> AppointmentMapper.toDto(a, true, queue.get(a.getId())))
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

    /**
     * Who is in the room, for the patient's waiting screen.
     *
     * <p><b>Read-only on purpose.</b> The waiting room polls this every few
     * seconds; if it stamped attendance the way {@link #stampAttendance} does,
     * simply having the screen open would record the patient as present without
     * them ever asking for the link - and a patient who opened the page and
     * walked away would be indistinguishable from one who attended.
     *
     * <p>Ownership is enforced the same way as everywhere else: the lookup is by
     * {@code (id, ownerId)}, and a miss is a 404 rather than a 403 so the
     * endpoint cannot be used to discover that an appointment exists.
     */
    @Transactional(readOnly = true)
    public RoomStatusResponse roomStatusAsPatient(Integer patientId, Integer appointmentId) {
        return toRoomStatus(appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found")));
    }

    @Transactional(readOnly = true)
    public RoomStatusResponse roomStatusAsDoctor(String doctorId, Integer appointmentId) {
        return toRoomStatus(appointmentRepository.findByIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found")));
    }

    private RoomStatusResponse toRoomStatus(Appointment a) {
        return new RoomStatusResponse(
                a.getId(),
                a.getDoctorJoinedAt() != null,
                a.getPatientJoinedAt() != null,
                a.isMeetingJoinable(),
                a.getMeetingJoinFrom() == null ? null
                        : a.getMeetingJoinFrom().format(JOIN_TIME),
                a.getStatus().toFrontend());
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
                .minusMinutes(settings.noShowGraceMinutes());
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
     * Published when a completed consultation's free revisit is claimed.
     *
     * <p>{@code fee} is the decision, not a description of it: this module rules
     * that the revisit costs nothing, and the payment module decides what a
     * zero-amount booking means for invoices and the doctor's payout. Same split
     * as the refund percentage on {@link AppointmentCancelledEvent} - the policy
     * is the number, and money moving is somebody else's concern.
     */
    public record FollowUpBookedEvent(Integer appointmentId, Integer parentAppointmentId,
                                      BigDecimal fee) {
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
