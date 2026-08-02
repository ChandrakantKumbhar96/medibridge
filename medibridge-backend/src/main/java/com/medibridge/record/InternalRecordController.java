package com.medibridge.record;

import com.medibridge.record.dto.RecordResponse;
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
@RequestMapping("/internal/records")
@RequiredArgsConstructor
public class InternalRecordController {

    private final RecordService recordService;

    /** Backs the chat-service's get_medical_records tool - see spring_client.py. */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public List<RecordResponse> listForPatient(@PathVariable Integer patientId) {
        return recordService.listForPatient(patientId);
    }
}
