package com.medibridge.appointment;

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
import com.medibridge.notification.EmailService;
import com.medibridge.notification.MeetingLinkService;
import com.medibridge.patient.PatientRepository;
import com.medibridge.patient.entity.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AppointmentService {

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
    private final EmailService emailService;

    // -------------------------------------------------------------- booking

    /**
     * Books a slot.
     *
     * <p>Three layers guard against double booking: a pessimistic lock on the
     * slot row, an {@code isBooked} check inside that lock, and the UNIQUE
     * constraint on {@code appointment.schedule_id}. Only the last is a real
     * guarantee - the first two exist so the failure surfaces as a clean
     * "slot no longer available" instead of a driver-level error.
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

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .schedule(slot)
                .appointmentDate(when)
                .status(AppointmentStatus.PENDING_PAYMENT)
                .consultType(request.consultType() == null ? "Consultation" : request.consultType())
                .reason(request.reason())
                .build();

        try {
            appointment = appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            // The unique constraint fired - another booking won the race.
            throw new ConflictException("That time slot has just been taken. Please pick another.");
        }

        slot.setIsBooked(true);
        scheduleRepository.save(slot);

        emailService.sendBookingRequested(patient.getEmail(), patient.getFullName(),
                doctor.getFullName(), AppointmentMapper.describe(appointment));

        return AppointmentMapper.toDto(appointment, true);
    }

    /**
     * Called by the payment module once a payment succeeds: the appointment
     * leaves PENDING_PAYMENT and becomes a real request for the doctor.
     */
    @Transactional
    public void markPaid(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.PENDING_PAYMENT) {
            appointment.setStatus(AppointmentStatus.REQUESTED);
            appointmentRepository.save(appointment);
        }
    }

    // ------------------------------------------------------- patient views

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

    /** Ownership is enforced by querying on (id, patientId), never id alone. */
    @Transactional
    public AppointmentResponse cancelAsPatient(Integer patientId, Integer appointmentId) {
        Appointment appointment = appointmentRepository
                .findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        return cancel(appointment, appointment.getPatient().getEmail(),
                appointment.getPatient().getFullName());
    }

    private AppointmentResponse cancel(Appointment appointment, String email, String name) {
        if (!appointment.getStatus().isCancellable()) {
            throw new BadRequestException(
                    "This appointment can no longer be cancelled (status: "
                            + appointment.getStatus().getDbValue() + ")");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        releaseSlot(appointment);

        emailService.sendCancellation(email, name, appointment.getDoctor().getFullName(),
                AppointmentMapper.describe(appointment));

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

    // -------------------------------------------------------- doctor views

    @Transactional(readOnly = true)
    public Map<String, List<AppointmentResponse>> getDoctorDashboard(String doctorId) {
        LocalDate today = LocalDate.now();

        List<AppointmentResponse> todays = appointmentRepository
                .findDoctorDay(doctorId, today.atStartOfDay(), today.plusDays(1).atStartOfDay())
                .stream()
                .filter(a -> a.getStatus().isActive())
                .map(a -> AppointmentMapper.toDto(a, true))
                .toList();

        List<AppointmentResponse> pending = appointmentRepository
                .findByDoctorIdAndStatusInOrderByAppointmentDateAsc(
                        doctorId, List.of(AppointmentStatus.REQUESTED))
                .stream().map(a -> AppointmentMapper.toDto(a, false)).toList();

        List<AppointmentResponse> completed = appointmentRepository
                .findByDoctorIdAndStatusInOrderByAppointmentDateAsc(
                        doctorId, List.of(AppointmentStatus.COMPLETED))
                .stream().map(a -> AppointmentMapper.toDto(a, false)).toList();

        Map<String, List<AppointmentResponse>> dashboard = new LinkedHashMap<>();
        dashboard.put("today", todays);
        dashboard.put("pending", pending);
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
     * Doctor accepts or rejects a request. On acceptance the consultation link
     * is generated and emailed - this is the only place a meeting link is created.
     */
    @Transactional
    public AppointmentResponse respond(String doctorId, Integer appointmentId, String action) {
        Appointment appointment = appointmentRepository
                .findByIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.REQUESTED
                && appointment.getStatus() != AppointmentStatus.RESCHEDULED) {
            throw new BadRequestException(
                    "Only pending requests can be accepted or rejected (status: "
                            + appointment.getStatus().getDbValue() + ")");
        }

        String normalised = action == null ? "" : action.trim().toLowerCase(Locale.ENGLISH);

        return switch (normalised) {
            case "accept", "accepted", "approve" -> accept(appointment);
            case "reject", "rejected", "decline" -> reject(appointment);
            case "complete", "completed" -> complete(appointment);
            default -> throw new BadRequestException(
                    "action must be one of: accept, reject, complete");
        };
    }

    private AppointmentResponse accept(Appointment appointment) {
        appointment.setStatus(AppointmentStatus.ACCEPTED);
        appointment.setMeetingLink(meetingLinkService.generate());
        appointment.setMeetingSentAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        emailService.sendMeetingLink(
                appointment.getPatient().getEmail(),
                appointment.getPatient().getFullName(),
                appointment.getDoctor().getFullName(),
                AppointmentMapper.describe(appointment),
                appointment.getMeetingLink());

        return AppointmentMapper.toDto(appointment, true);
    }

    private AppointmentResponse reject(Appointment appointment) {
        appointment.setStatus(AppointmentStatus.REJECTED);
        appointmentRepository.save(appointment);
        releaseSlot(appointment);

        emailService.sendCancellation(
                appointment.getPatient().getEmail(),
                appointment.getPatient().getFullName(),
                appointment.getDoctor().getFullName(),
                AppointmentMapper.describe(appointment));

        return AppointmentMapper.toDto(appointment, false);
    }

    private AppointmentResponse complete(Appointment appointment) {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
        return AppointmentMapper.toDto(appointment, false);
    }

    @Transactional
    public AppointmentResponse cancelAsDoctor(String doctorId, Integer appointmentId) {
        Appointment appointment = appointmentRepository
                .findByIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        return cancel(appointment, appointment.getPatient().getEmail(),
                appointment.getPatient().getFullName());
    }
}
