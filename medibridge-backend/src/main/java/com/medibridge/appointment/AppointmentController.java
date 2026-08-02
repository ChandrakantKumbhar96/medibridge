package com.medibridge.appointment;

import com.medibridge.appointment.dto.AppointmentResponse;
import com.medibridge.appointment.dto.BookAppointmentRequest;
import com.medibridge.appointment.dto.NextAvailableResponse;
import com.medibridge.appointment.dto.RoomStatusResponse;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * The soonest open slot in the configured window, for a patient who would
     * rather be matched than browse. 204 when nothing qualifies, rather than
     * a null body the frontend would have to special-case.
     *
     * <p>{@code specialization} is a name ("Cardiologist"), not an id - the
     * same value the specialty chips on FindDoctors already hold, so a filter
     * the patient picked there carries straight through without a lookup.
     */
    @GetMapping("/next-available")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<NextAvailableResponse> nextAvailable(
            @RequestParam(value = "specialization", required = false) String specialization) {
        return appointmentService.nextAvailable(specialization)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@CurrentUser SecurityUser me,
                                    @Valid @RequestBody BookAppointmentRequest request) {
        return appointmentService.book(me.idAsInt(), request);
    }

    /**
     * Books the one free revisit a completed consultation earns.
     *
     * <p>Body: {@code { "schedule_id": 123 }} - the same shape as reschedule.
     *
     * <p>Deliberately its own endpoint rather than a flag on POST /appointments:
     * this one takes no fee, no doctor and no consult type from the client. All
     * three are inherited from the parent consultation, so there is nothing a
     * caller could tamper with to get a free slot with a doctor they never saw.
     *
     * <p>Returns 409 when the revisit is already used or the slot is taken.
     */
    @PostMapping("/{appointmentId}/follow-up")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse bookFollowUp(@CurrentUser SecurityUser me,
                                            @PathVariable Integer appointmentId,
                                            @RequestBody Map<String, Integer> body) {
        Integer scheduleId = body == null ? null : body.get("schedule_id");
        if (scheduleId == null) {
            throw new BadRequestException("schedule_id is required");
        }
        return appointmentService.bookFollowUp(me.idAsInt(), appointmentId, scheduleId);
    }

    /**
     * Cancels and refunds automatically per policy: a doctor cancelling always
     * refunds in full; a patient cancelling gets the full amount outside the
     * free-cancellation window and a partial refund inside it.
     */
    @PatchMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public AppointmentResponse cancel(@CurrentUser SecurityUser me,
                                      @PathVariable Integer appointmentId,
                                      @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");

        return switch (me.getUserType()) {
            case PATIENT -> appointmentService.cancelAsPatient(me.idAsInt(), appointmentId, reason);
            case DOCTOR -> appointmentService.cancelAsDoctor(me.getId(), appointmentId, reason);
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

    /**
     * Marks a consultation done.
     *
     * <p>There is deliberately no accept/reject endpoint. Publishing a slot is
     * the doctor's agreement to be booked, so payment confirms the appointment
     * outright - the same model Practo and Apollo use. A doctor who genuinely
     * cannot attend cancels, which refunds the patient in full.
     */
    @PatchMapping("/{appointmentId}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public AppointmentResponse complete(@CurrentUser SecurityUser me,
                                        @PathVariable Integer appointmentId) {
        return appointmentService.complete(me.getId(), appointmentId);
    }

    /**
     * Returns the video consultation room URL, minted on demand.
     *
     * <p>The link is never sent in list responses or emailed — it is a bearer
     * secret. It is issued here only to the patient or treating doctor, for a
     * paid and confirmed appointment, inside the join window. Outside the
     * window this returns 400 with the opening time.
     */
    @GetMapping("/{appointmentId}/join")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public Map<String, String> join(@CurrentUser SecurityUser me,
                                    @PathVariable Integer appointmentId) {
        String link = switch (me.getUserType()) {
            case PATIENT -> appointmentService.getJoinLinkAsPatient(me.idAsInt(), appointmentId);
            case DOCTOR -> appointmentService.getJoinLinkAsDoctor(me.getId(), appointmentId);
            default -> throw new BadRequestException("Unsupported role for this action");
        };
        return Map.of("meeting_link", link);
    }

    /**
     * Who is currently in the consultation room.
     *
     * <p>Polled by the patient's waiting room, so it is deliberately read-only —
     * unlike {@code /join} it records nothing. It never returns the room URL.
     */
    @GetMapping("/{appointmentId}/room-status")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public RoomStatusResponse roomStatus(@CurrentUser SecurityUser me,
                                         @PathVariable Integer appointmentId) {
        return switch (me.getUserType()) {
            case PATIENT -> appointmentService.roomStatusAsPatient(me.idAsInt(), appointmentId);
            case DOCTOR -> appointmentService.roomStatusAsDoctor(me.getId(), appointmentId);
            default -> throw new BadRequestException("Unsupported role for this action");
        };
    }

    /**
     * Moves an appointment to a different slot with the same doctor.
     *
     * <p>Body: {@code { "schedule_id": 123 }}
     *
     * <p>The appointment id survives, so the payment carries across and no
     * refund/recharge cycle is needed. Patient reschedules are limited by
     * notice period and count; a doctor rescheduling is not restricted.
     */
    @PatchMapping("/{appointmentId}/reschedule")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public AppointmentResponse reschedule(@CurrentUser SecurityUser me,
                                          @PathVariable Integer appointmentId,
                                          @RequestBody Map<String, Integer> body) {

        Integer newScheduleId = body.get("schedule_id");
        if (newScheduleId == null) {
            throw new BadRequestException("schedule_id is required");
        }

        return switch (me.getUserType()) {
            case PATIENT -> appointmentService.reschedule(appointmentId, newScheduleId,
                    Appointment.ActorRole.PATIENT, me.idAsInt(), null);
            case DOCTOR -> appointmentService.reschedule(appointmentId, newScheduleId,
                    Appointment.ActorRole.DOCTOR, null, me.getId());
            default -> throw new BadRequestException("Unsupported role for this action");
        };
    }
}
