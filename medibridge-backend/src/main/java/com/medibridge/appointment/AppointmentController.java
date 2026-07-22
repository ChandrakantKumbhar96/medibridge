package com.medibridge.appointment;

import com.medibridge.appointment.dto.AppointmentResponse;
import com.medibridge.appointment.dto.BookAppointmentRequest;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints called by services/appointmentService.js.
 *
 * <p>Every handler takes the caller's id from {@link CurrentUser}; no endpoint
 * accepts a patient or doctor id from the client. That is what prevents one
 * patient cancelling another's appointment by changing a number.
 */
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // ------------------------------------------------------------- patient

    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public Map<String, List<AppointmentResponse>> myAppointments(@CurrentUser SecurityUser me) {
        return appointmentService.getPatientAppointments(me.idAsInt());
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@CurrentUser SecurityUser me,
                                    @Valid @RequestBody BookAppointmentRequest request) {
        return appointmentService.book(me.idAsInt(), request);
    }

    @PatchMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public AppointmentResponse cancel(@CurrentUser SecurityUser me,
                                      @PathVariable Integer appointmentId) {
        return switch (me.getUserType()) {
            case PATIENT -> appointmentService.cancelAsPatient(me.idAsInt(), appointmentId);
            case DOCTOR -> appointmentService.cancelAsDoctor(me.getId(), appointmentId);
            default -> throw new BadRequestException("Unsupported role for this action");
        };
    }

    // -------------------------------------------------------------- doctor

    @GetMapping("/doctor/dashboard")
    @PreAuthorize("hasRole('DOCTOR')")
    public Map<String, List<AppointmentResponse>> doctorDashboard(@CurrentUser SecurityUser me) {
        return appointmentService.getDoctorDashboard(me.getId());
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<AppointmentResponse> doctorAppointments(@CurrentUser SecurityUser me) {
        return appointmentService.getDoctorAppointments(me.getId());
    }

    /** Body: {@code { "action": "accept" | "reject" | "complete" }} */
    @PatchMapping("/{appointmentId}/respond")
    @PreAuthorize("hasRole('DOCTOR')")
    public AppointmentResponse respond(@CurrentUser SecurityUser me,
                                       @PathVariable Integer appointmentId,
                                       @RequestBody Map<String, String> body) {
        return appointmentService.respond(me.getId(), appointmentId, body.get("action"));
    }
}
