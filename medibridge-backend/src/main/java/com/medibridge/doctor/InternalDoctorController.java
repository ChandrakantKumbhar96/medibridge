package com.medibridge.doctor;

import com.medibridge.doctor.dto.DoctorResponse;
import com.medibridge.doctor.dto.ScheduleDayDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Called only by trusted internal services via the {@code X-Internal-Api-Key}
 * header - see InternalApiKeyAuthFilter. Same trust model as
 * {@link com.medibridge.appointment.InternalAppointmentController}: no
 * ownership scoping by design, safe only because reaching this controller at
 * all requires the shared secret, never a user-supplied JWT.
 */
@RestController
@RequestMapping("/internal/doctors")
@RequiredArgsConstructor
@Tag(name = "Internal - Doctors", description = "Backs the chat assistant's tools. Requires X-Internal-Api-Key.")
public class InternalDoctorController {

    private final DoctorService doctorService;

    /** Backs the chat-service's get_my_profile tool (doctor role) - see spring_client.py. */
    @GetMapping("/{doctorId}/profile")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @Operation(summary = "Get a doctor's own profile", description = "Backs the chat-service's get_my_profile tool (doctor role).")
    public DoctorResponse getProfile(@PathVariable String doctorId) {
        return doctorService.getOwnProfile(doctorId);
    }

    /** Backs the chat-service's get_weekly_schedule tool - see spring_client.py. */
    @GetMapping("/{doctorId}/weekly-schedule")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @Operation(summary = "Get a doctor's weekly schedule", description = "Backs the chat-service's get_weekly_schedule tool.")
    public List<ScheduleDayDto> getWeeklySchedule(@PathVariable String doctorId) {
        return doctorService.getWeeklySchedule(doctorId);
    }

    /** Backs the chat-service's get_treated_patients tool - see spring_client.py. */
    @GetMapping("/{doctorId}/treated-patients")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @Operation(summary = "List patients a doctor has treated", description = "Backs the chat-service's get_treated_patients tool.")
    public List<Map<String, Object>> getTreatedPatients(@PathVariable String doctorId) {
        return doctorService.getTreatedPatients(doctorId);
    }

    /**
     * Backs the chat-service's search_doctors tool - see spring_client.py.
     * Same query this service's own /doctors browsing endpoint runs
     * (DoctorController.listDoctors) - only ACTIVE doctors are ever returned.
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @Operation(summary = "Search doctors", description = "Backs the chat-service's search_doctors tool. Only ACTIVE doctors are returned.")
    public List<DoctorResponse> search(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String search) {
        return doctorService.listDoctors(specialization, search);
    }

    /**
     * Backs the chat-service's get_doctor_profile tool - see spring_client.py.
     * Distinct from {@code /profile} above: this is the public-browsing view
     * (404s for a suspended/inactive doctor, same as DoctorController.getDoctor),
     * not the caller's own account.
     */
    @GetMapping("/{doctorId}/public-profile")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @Operation(summary = "Get a doctor's public profile", description = "Backs the chat-service's get_doctor_profile tool.")
    public DoctorResponse getPublicProfile(@PathVariable String doctorId) {
        return doctorService.getDoctor(doctorId);
    }
}
