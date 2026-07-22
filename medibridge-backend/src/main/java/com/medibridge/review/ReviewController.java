package com.medibridge.review;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.review.dto.RatingRequest;
import com.medibridge.review.dto.RatingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse submit(@CurrentUser SecurityUser me,
                                 @Valid @RequestBody RatingRequest request) {
        return reviewService.submit(me.idAsInt(), request);
    }

    @GetMapping("/reviews/appointment/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT')")
    public RatingResponse forAppointment(@CurrentUser SecurityUser me,
                                         @PathVariable Integer appointmentId) {
        return reviewService.getForAppointment(me.idAsInt(), appointmentId);
    }

    /** Public-facing reviews shown on a doctor's profile. */
    @GetMapping("/doctors/{doctorId}/reviews")
    public List<RatingResponse> forDoctor(@PathVariable String doctorId) {
        return reviewService.listForDoctor(doctorId);
    }
}
