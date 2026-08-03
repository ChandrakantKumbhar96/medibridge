package com.medibridge.prescription;

import com.medibridge.prescription.dto.PrescriptionResponse;
import com.medibridge.prescription.dto.PrescriptionStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Called only by trusted internal services via the {@code X-Internal-Api-Key}
 * header - see InternalApiKeyAuthFilter. Same trust model as
 * {@link com.medibridge.appointment.InternalAppointmentController}: no
 * ownership scoping by design, safe only because reaching this controller at
 * all requires the shared secret, never a user-supplied JWT.
 */
@RestController
@RequestMapping("/internal/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Internal - Prescriptions", description = "Backs the chat assistant's tools. Requires X-Internal-Api-Key.")
public class InternalPrescriptionController {

    private final PrescriptionService prescriptionService;

    /** Backs the chat-service's get_prescription_status tool - see spring_client.py. */
    @GetMapping("/patient/{patientId}/latest")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @Operation(summary = "Get a patient's latest prescription status", description = "Backs the chat-service's get_prescription_status tool.")
    public PrescriptionStatusResponse getLatest(@PathVariable Integer patientId) {
        return prescriptionService.latestForPatient(patientId);
    }

    /** Backs the chat-service's get_prescriptions tool - see spring_client.py. */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @Operation(summary = "List a patient's prescriptions", description = "Backs the chat-service's get_prescriptions tool.")
    public List<PrescriptionResponse> listForPatient(@PathVariable Integer patientId) {
        return prescriptionService.listForPatient(patientId);
    }
}
