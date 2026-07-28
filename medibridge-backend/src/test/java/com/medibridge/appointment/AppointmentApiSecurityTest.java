package com.medibridge.appointment;

import com.medibridge.appointment.entity.Appointment;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.patient.entity.Patient;
import com.medibridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// Spring Boot 4 moved MockMvc autoconfiguration out of spring-boot-test-autoconfigure
// into the per-technology module spring-boot-webmvc-test. Same split that caught
// Flyway: the old org.springframework.boot.test.autoconfigure.web.servlet package
// no longer exists.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization at the HTTP edge.
 *
 * <p>Two rules are asserted here that URL-based security cannot express:
 * a role may reach an endpoint but still not the row behind it, and an
 * ownership miss returns <b>404, not 403</b>. A 403 confirms the resource
 * exists, which hands an attacker a working id oracle.
 */
@AutoConfigureMockMvc
class AppointmentApiSecurityTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("anonymous callers get 401")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/appointments/patient"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a patient cannot reach doctor-only endpoints")
    void roleBoundariesHold() throws Exception {
        Patient patient = newPatient();

        mockMvc.perform(get("/appointments/doctor/dashboard").with(user(asPatient(patient))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a doctor cannot reach patient-only endpoints")
    void roleBoundariesHoldBothWays() throws Exception {
        Doctor doctor = newDoctor();

        mockMvc.perform(get("/appointments/patient").with(user(asDoctor(doctor))))
                .andExpect(status().isForbidden());
    }

    /**
     * The important one. Both callers are authenticated patients with the same
     * role, so no URL rule can separate them - only the {@code (id, ownerId)}
     * query in the service can.
     */
    @Test
    @DisplayName("cancelling someone else's appointment returns 404, never 403")
    void ownershipMissIsNotFound() throws Exception {
        Doctor doctor = newDoctor();
        Patient owner = newPatient();
        Patient stranger = newPatient();

        Appointment appointment = paidAppointment(owner, slotInDays(doctor, 10));

        mockMvc.perform(patch("/appointments/{id}/cancel", appointment.getId())
                        .with(user(asPatient(stranger)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"not mine\"}"))
                .andExpect(status().isNotFound());

        // And it really is untouched, not merely reported as missing.
        Appointment stillThere = appointmentRepository.findById(appointment.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(stillThere.getCancelledAt()).isNull();
    }

    @Test
    @DisplayName("a doctor cannot act on another doctor's appointment")
    void doctorOwnershipIsScopedToo() throws Exception {
        Doctor treating = newDoctor();
        Doctor other = newDoctor();
        Patient patient = newPatient();

        Appointment appointment = paidAppointment(patient, slotInDays(treating, 10));

        mockMvc.perform(patch("/appointments/{id}/complete", appointment.getId())
                        .with(user(asDoctor(other))))
                .andExpect(status().isNotFound());
    }

    /**
     * The room URL is a bearer secret: anyone holding it can walk into the
     * consultation. It must never appear in a list payload, only be handed out
     * by the dedicated join endpoint inside the allowed window.
     */
    @Test
    @DisplayName("list responses never carry the meeting link")
    void meetingLinkIsNotLeakedInLists() throws Exception {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();
        paidAppointment(patient, slotInDays(doctor, 3));

        mockMvc.perform(get("/appointments/patient").with(user(asPatient(patient))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming[*].meeting_link").doesNotExist());
    }

    @Test
    @DisplayName("the join link is refused outside its window")
    void joinIsWindowed() throws Exception {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();

        // Days away, so the room has not opened yet.
        Appointment appointment = paidAppointment(patient, slotInDays(doctor, 5));

        mockMvc.perform(get("/appointments/{id}/join", appointment.getId())
                        .with(user(asPatient(patient))))
                .andExpect(status().isBadRequest());

        // A refused request must not count as attendance - otherwise anyone
        // could clear their no-show record by pressing Join at the wrong time.
        Appointment after = appointmentRepository.findById(appointment.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(after.getPatientJoinedAt()).isNull();
    }
}
