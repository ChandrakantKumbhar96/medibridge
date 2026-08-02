package com.medibridge.patient;

import com.medibridge.patient.dto.FamilyMemberResponse;
import com.medibridge.patient.dto.PatientProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
@RequestMapping("/internal/patients")
@RequiredArgsConstructor
public class InternalPatientController {

    private final PatientService patientService;
    private final FamilyMemberService familyMemberService;

    /** Backs the chat-service's get_my_profile tool - see spring_client.py. */
    @GetMapping("/{patientId}/profile")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public PatientProfileResponse getProfile(@PathVariable Integer patientId) {
        return patientService.getProfile(patientId);
    }

    /** Backs the chat-service's get_my_stats tool - see spring_client.py. */
    @GetMapping("/{patientId}/stats")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public Map<String, Object> getStats(@PathVariable Integer patientId) {
        return patientService.getStats(patientId);
    }

    /** Backs the chat-service's get_family_members tool - see spring_client.py. */
    @GetMapping("/{patientId}/family-members")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public List<FamilyMemberResponse> getFamilyMembers(@PathVariable Integer patientId) {
        return familyMemberService.list(patientId);
    }
}
