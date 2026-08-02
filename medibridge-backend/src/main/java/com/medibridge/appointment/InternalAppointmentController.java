package com.medibridge.appointment;

import com.medibridge.appointment.dto.AppointmentResponse;
import com.medibridge.appointment.dto.NextAppointmentResponse;
import com.medibridge.appointment.dto.NextAvailableResponse;
import com.medibridge.appointment.dto.ReminderCandidateResponse;
import com.medibridge.appointment.dto.RescheduleStatusResponse;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.common.util.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Called only by trusted internal services (currently the .NET notify
 * service's {@code SpringApiClient}) via the {@code X-Internal-Api-Key}
 * header - see InternalApiKeyAuthFilter.
 *
 * <p>Unlike {@link AppointmentController}, this has no ownership scoping by
 * design: the caller isn't a patient or doctor acting on their own behalf,
 * it's a backend job (e.g. NoShowReminderJob) that already knows the
 * appointment id it needs. Trusting that id here is safe only because
 * reaching this controller at all requires the shared secret, never a
 * user-supplied JWT - both the URL rule and @PreAuthorize below reject
 * anything else.
 */
@RestController
@RequestMapping("/internal/appointments")
@RequiredArgsConstructor
public class InternalAppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public AppointmentResponse getById(@PathVariable Integer appointmentId) {
        Appointment appointment = appointmentService.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        return AppointmentMapper.toDto(appointment, false);
    }

    /**
     * Backs the .NET notify-service's {@code NoShowReminderJob}.
     *
     * <p>Same candidate set as {@code AppointmentScheduler.sendUpcomingReminders}
     * on the Spring side - the two are not coordinated, so running both would
     * double-send. This exists for the notify-service to poll instead of, not
     * alongside, the in-process Spring reminder sweep.
     */
    /** Backs the chat-service's get_appointment_count tool - see spring_client.py. */
    @GetMapping("/patient/{patientId}/count")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public long getPatientAppointmentCount(@PathVariable Integer patientId) {
        return appointmentService.countUpcomingForPatient(patientId);
    }

    /** Backs the chat-service's get_appointment_count tool - see spring_client.py. */
    @GetMapping("/doctor/{doctorId}/count")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public long getDoctorAppointmentCount(@PathVariable String doctorId) {
        return appointmentService.countUpcomingForDoctor(doctorId);
    }

    /** Backs the chat-service's get_today_queue_count tool - see spring_client.py. */
    @GetMapping("/doctor/{doctorId}/today-count")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public long getDoctorTodayCount(@PathVariable String doctorId) {
        return appointmentService.countTodayForDoctor(doctorId);
    }

    /** Backs the chat-service's get_next_appointment tool - see spring_client.py. */
    @GetMapping("/patient/{patientId}/next")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public NextAppointmentResponse getNextForPatient(@PathVariable Integer patientId) {
        return appointmentService.nextForPatient(patientId);
    }

    /** Backs the chat-service's get_upcoming_appointments tool - see spring_client.py. */
    @GetMapping("/patient/{patientId}/upcoming")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public List<AppointmentResponse> getUpcomingForPatient(@PathVariable Integer patientId) {
        return appointmentService.getPatientAppointments(patientId).get("upcoming");
    }

    /** Backs the chat-service's get_appointment_history tool - see spring_client.py. */
    @GetMapping("/patient/{patientId}/history")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public List<AppointmentResponse> getHistoryForPatient(@PathVariable Integer patientId) {
        return appointmentService.getPatientAppointments(patientId).get("past");
    }

    /** Backs the chat-service's get_next_appointment tool - see spring_client.py. */
    @GetMapping("/doctor/{doctorId}/next")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public NextAppointmentResponse getNextForDoctor(@PathVariable String doctorId) {
        return appointmentService.nextForDoctor(doctorId);
    }

    /** Backs the chat-service's get_reschedule_status tool - see spring_client.py. */
    @GetMapping("/patient/{patientId}/reschedule-status")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public RescheduleStatusResponse getRescheduleStatus(@PathVariable Integer patientId) {
        return appointmentService.rescheduleStatusForPatient(patientId);
    }

    /** Backs the chat-service's get_doctor_dashboard tool - see spring_client.py. */
    @GetMapping("/doctor/{doctorId}/dashboard")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public Map<String, List<AppointmentResponse>> getDoctorDashboard(@PathVariable String doctorId) {
        return appointmentService.getDoctorDashboard(doctorId);
    }

    /** Backs the chat-service's get_next_available_slot tool - see spring_client.py. */
    @GetMapping("/next-available")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public NextAvailableResponse getNextAvailable(
            @RequestParam(required = false) String specialization) {
        return appointmentService.nextAvailable(specialization)
                .orElseThrow(() -> new ResourceNotFoundException("No slot available"));
    }

    @GetMapping("/reminder-candidates")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public List<ReminderCandidateResponse> getReminderCandidates() {
        return appointmentService.findAppointmentsNeedingReminder().stream()
                .map(a -> new ReminderCandidateResponse(
                        a.getId(),
                        PhoneNumbers.toE164(a.getPatient().getPhone()),
                        a.getPatient().getFullName(),
                        a.getDoctor().getFullName(),
                        a.getAppointmentDate().toString()))
                .toList();
    }
}
