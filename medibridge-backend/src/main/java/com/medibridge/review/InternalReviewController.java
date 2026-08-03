package com.medibridge.review;

import com.medibridge.review.dto.RatingResponse;
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
@RequestMapping("/internal/reviews")
@RequiredArgsConstructor
@Tag(name = "Internal - Reviews", description = "Backs the chat assistant's tools. Requires X-Internal-Api-Key.")
public class InternalReviewController {

    private final ReviewService reviewService;

    /** Backs the chat-service's get_my_reviews tool - see spring_client.py. */
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @Operation(summary = "List a doctor's reviews", description = "Backs the chat-service's get_my_reviews tool.")
    public List<RatingResponse> listForDoctor(@PathVariable String doctorId) {
        return reviewService.listForDoctor(doctorId);
    }
}
