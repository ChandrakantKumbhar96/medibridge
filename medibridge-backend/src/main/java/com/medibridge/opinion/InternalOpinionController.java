package com.medibridge.opinion;

import com.medibridge.opinion.dto.OpinionResponse;
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
@RequestMapping("/internal/opinions")
@RequiredArgsConstructor
public class InternalOpinionController {

    private final OpinionService opinionService;

    /** Backs the chat-service's get_second_opinions tool - see spring_client.py. */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public List<OpinionResponse> listForPatient(@PathVariable Integer patientId) {
        return opinionService.listForPatient(patientId);
    }
}
