package com.medibridge.review;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.review.dto.RatingRequest;
import com.medibridge.review.dto.RatingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Patient ratings and reviews of doctors")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a review", description = "Rates a completed appointment. Patient only.")
    public RatingResponse submit(@CurrentUser SecurityUser me,
                                 @Valid @RequestBody RatingRequest request) {
        return reviewService.submit(me.idAsInt(), request);
    }

    @GetMapping("/reviews/appointment/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get the review for an appointment", description = "Returns the caller's own review for one of their appointments.")
    public RatingResponse forAppointment(@CurrentUser SecurityUser me,
                                         @PathVariable Integer appointmentId) {
        return reviewService.getForAppointment(me.idAsInt(), appointmentId);
    }

    @GetMapping("/doctors/{doctorId}/reviews")
    @Operation(summary = "List a doctor's reviews", description = "Public-facing reviews shown on a doctor's profile.")
    public List<RatingResponse> forDoctor(@PathVariable String doctorId) {
        return reviewService.listForDoctor(doctorId);
    }
}
