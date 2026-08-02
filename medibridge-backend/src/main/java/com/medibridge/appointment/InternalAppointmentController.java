package com.medibridge.appointment;

import com.medibridge.appointment.dto.AppointmentResponse;
import com.medibridge.appointment.dto.ReminderCandidateResponse;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.common.util.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
