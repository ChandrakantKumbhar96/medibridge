package com.medibridge.patient;

import com.medibridge.patient.dto.FamilyMemberResponse;
import com.medibridge.patient.dto.PatientProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Internal - Patients", description = "Backs the chat assistant's tools. Requires X-Internal-Api-Key.")
public class InternalPatientController {

    private final PatientService patientService;
    private final FamilyMemberService familyMemberService;

    /** Backs the chat-service's get_my_profile tool - see spring_client.py. */
    @GetMapping("/{patientId}/profile")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @Operation(summary = "Get a patient's profile", description = "Backs the chat-service's get_my_profile tool.")
    public PatientProfileResponse getProfile(@PathVariable Integer patientId) {
        return patientService.getProfile(patientId);
    }

    /** Backs the chat-service's get_my_stats tool - see spring_client.py. */
    @GetMapping("/{patientId}/stats")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @Operation(summary = "Get a patient's dashboard stats", description = "Backs the chat-service's get_my_stats tool.")
    public Map<String, Object> getStats(@PathVariable Integer patientId) {
        return patientService.getStats(patientId);
    }

    /** Backs the chat-service's get_family_members tool - see spring_client.py. */
    @GetMapping("/{patientId}/family-members")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @Operation(summary = "List a patient's family members", description = "Backs the chat-service's get_family_members tool.")
    public List<FamilyMemberResponse> getFamilyMembers(@PathVariable Integer patientId) {
        return familyMemberService.list(patientId);
    }
}
