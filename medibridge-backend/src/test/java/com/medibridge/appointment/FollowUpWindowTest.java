package com.medibridge.appointment;

import com.medibridge.admin.SettingsProvider;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.patient.entity.Patient;
import com.medibridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The free follow-up window: one revisit, same doctor, N days, no charge.
 *
 * <p>The zero fee is asserted on the published {@code FollowUpBookedEvent}
 * rather than on a payment row. The price being nil <em>is</em> the policy; what
 * the payment module does with a zero-amount booking is its own decision, and a
 * test that reached into it would fail for reasons that have nothing to do with
 * this rule.
 *
 * <p>The "one per consultation" limit is a UNIQUE index, not a flag - so the
 * second attempt is expected to be refused whether or not the service check
 * catches it first.
 */
@RecordApplicationEvents
class FollowUpWindowTest extends AbstractIntegrationTest {

    @Autowired SettingsProvider settings;
    @Autowired ApplicationEvents events;

    @Test
    @DisplayName("inside the window the revisit costs nothing and is confirmed outright")
    void followUpInsideWindowIsFree() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();
        Appointment parent = completedConsultation(patient, doctor);

        var followUp = appointmentService.bookFollowUp(
                patient.getId(), parent.getId(), slotInDays(doctor, 3).getId());

        var booked = followUpEvents(followUp.appointmentId());
        assertThat(booked).hasSize(1);
        assertThat(booked.getFirst().fee())
                .as("the zero fee is the policy decision, announced not enacted")
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(booked.getFirst().parentAppointmentId()).isEqualTo(parent.getId());

        Appointment saved = appointmentRepository.findById(followUp.appointmentId()).orElseThrow();
        assertThat(saved.getStatus())
                .as("nothing to pay, so it lands where a paid booking lands")
                .isEqualTo(AppointmentStatus.ACCEPTED);
        assertThat(saved.getMeetingLink())
                .as("skipping payment must not skip the consultation room")
                .isNotNull();
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getParentAppointment().getId()).isEqualTo(parent.getId());
    }

    /**
     * The field the appointments screen renders the button from. Sent on the
     * completed consultation itself so the UI needs no second call, and dropped
     * entirely once the revisit is spent - absence is the whole signal.
     */
    @Test
    @DisplayName("the completed row carries its own deadline, and loses it once used")
    void eligibilityIsAdvertisedOnThePastRow() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();
        Appointment parent = completedConsultation(patient, doctor);

        var past = appointmentService.getPatientAppointments(patient.getId()).get("past");
        assertThat(past).anySatisfy(a -> {
            assertThat(a.appointmentId()).isEqualTo(parent.getId());
            assertThat(a.followUpEligibleUntil()).isNotNull();
        });

        appointmentService.bookFollowUp(
                patient.getId(), parent.getId(), slotInDays(doctor, 3).getId());

        assertThat(appointmentService.getPatientAppointments(patient.getId()).get("past"))
                .filteredOn(a -> a.appointmentId().equals(parent.getId()))
                .allSatisfy(a -> assertThat(a.followUpEligibleUntil())
                        .as("the revisit is spent, so the button must stop being offered")
                        .isNull());
    }

    /**
     * Backdating {@code completed_at} rather than waiting: the window runs from
     * completion, so moving that one column is the whole of "a week has passed".
     */
    @Test
    @DisplayName("past the window the revisit is refused")
    void followUpOutsideWindowIsRefused() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();
        Appointment parent = completedConsultation(patient, doctor);

        parent.setCompletedAt(LocalDateTime.now()
                .minusDays(settings.followUpWindowDays() + 1L));
        appointmentRepository.save(parent);

        DoctorSchedule slot = slotInDays(doctor, 3);
        assertThatThrownBy(() -> appointmentService.bookFollowUp(
                patient.getId(), parent.getId(), slot.getId()))
                .hasMessageContaining("window");
    }

    @Test
    @DisplayName("one consultation yields one revisit, not two")
    void secondFollowUpOffTheSameParentIsRefused() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();
        Appointment parent = completedConsultation(patient, doctor);

        appointmentService.bookFollowUp(
                patient.getId(), parent.getId(), slotInDays(doctor, 3).getId());

        DoctorSchedule another = slotInDays(doctor, 4);
        assertThatThrownBy(() -> appointmentService.bookFollowUp(
                patient.getId(), parent.getId(), another.getId()))
                .hasMessageContaining("already used");
    }

    @Test
    @DisplayName("a revisit with a doctor who never saw you is refused")
    void followUpWithAnotherDoctorIsRefused() {
        Doctor doctor = newDoctor();
        Doctor other = newDoctor();
        Patient patient = newPatient();
        Appointment parent = completedConsultation(patient, doctor);

        DoctorSchedule foreign = slotInDays(other, 3);
        assertThatThrownBy(() -> appointmentService.bookFollowUp(
                patient.getId(), parent.getId(), foreign.getId()))
                .hasMessageContaining("same doctor");
    }

    /**
     * Ownership is the {@code (id, ownerId)} lookup and a miss is a 404 - a 403
     * would confirm that someone else's appointment exists.
     */
    @Test
    @DisplayName("another patient's consultation earns you nothing")
    void followUpIsOwnershipScoped() {
        Doctor doctor = newDoctor();
        Patient owner = newPatient();
        Patient stranger = newPatient();
        Appointment parent = completedConsultation(owner, doctor);

        DoctorSchedule slot = slotInDays(doctor, 3);
        assertThatThrownBy(() -> appointmentService.bookFollowUp(
                stranger.getId(), parent.getId(), slot.getId()))
                .hasMessageContaining("not found");
    }

    // ---------------------------------------------------------------- setup

    /**
     * A paid consultation the doctor has closed out.
     *
     * <p>Booking refuses a past slot, so the appointment is created ahead and
     * then moved back - {@code complete()} will not close a consultation whose
     * time has not arrived, and a test that slept would be slow and still flaky.
     */
    private Appointment completedConsultation(Patient patient, Doctor doctor) {
        Appointment a = paidAppointment(patient, slotInDays(doctor, 2));
        a.setAppointmentDate(LocalDateTime.now().minusHours(2));
        appointmentRepository.save(a);

        appointmentService.complete(doctor.getId(), a.getId());
        return appointmentRepository.findById(a.getId()).orElseThrow();
    }

    private List<AppointmentService.FollowUpBookedEvent> followUpEvents(Integer appointmentId) {
        return events.stream(AppointmentService.FollowUpBookedEvent.class)
                .filter(e -> e.appointmentId().equals(appointmentId))
                .toList();
    }
}
