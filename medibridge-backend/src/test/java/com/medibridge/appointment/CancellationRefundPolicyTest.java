package com.medibridge.appointment;

import com.medibridge.appointment.entity.Appointment;
import com.medibridge.admin.SettingsProvider;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.patient.entity.Patient;
import com.medibridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The refund matrix.
 *
 * <p>Asserted on the published {@code AppointmentCancelledEvent} rather than on
 * money actually moving. The refund percentage <em>is</em> the policy decision;
 * what the payment gateway then does with it is the payment module's concern
 * and is deliberately not under test here.
 */
@RecordApplicationEvents
class CancellationRefundPolicyTest extends AbstractIntegrationTest {

    @Autowired ApplicationEvents events;
    @Autowired SettingsProvider settings;

    @Test
    @DisplayName("patient cancels outside the window: full refund")
    void fullRefundOutsideTheWindow() {
        Appointment appointment = paidAppointment(newPatient(), slotInDays(newDoctor(), 10));

        appointmentService.cancelAsPatient(
                appointment.getPatient().getId(), appointment.getId(), "changed my mind");

        assertThat(refundPercentFor(appointment.getId())).isEqualTo(100);
    }

    @Test
    @DisplayName("patient cancels inside the window: partial refund")
    void partialRefundInsideTheWindow() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();

        // Comfortably inside free_cancellation_hours, whatever it is configured
        // to be - hardcoding 24 would silently stop testing the rule the day
        // someone changes the setting.
        int cutoff = settings.freeCancellationHours();
        DoctorSchedule slot = newSlot(doctor,
                LocalDateTime.now().plusHours(Math.max(1, cutoff - 1)));

        Appointment appointment = paidAppointment(patient, slot);

        appointmentService.cancelAsPatient(
                patient.getId(), appointment.getId(), "something came up");

        assertThat(refundPercentFor(appointment.getId()))
                .isEqualTo(settings.partialRefundPercent());
    }

    @Test
    @DisplayName("doctor cancels: always 100%, however late")
    void doctorCancellationAlwaysRefundsInFull() {
        Doctor doctor = newDoctor();
        DoctorSchedule slot = newSlot(doctor, LocalDateTime.now().plusMinutes(45));
        Appointment appointment = paidAppointment(newPatient(), slot);

        appointmentService.cancelAsDoctor(doctor.getId(), appointment.getId(), "emergency");

        assertThat(refundPercentFor(appointment.getId())).isEqualTo(100);
    }

    /**
     * The fairness rule: a doctor-initiated move leaves the patient at a time
     * they never chose, and often inside the penalty window. Billing them for
     * that would be charging them for the doctor's schedule change.
     */
    @Test
    @DisplayName("doctor moved it, patient then cancels late: still 100%")
    void doctorRescheduleWaivesTheCutoff() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();

        DoctorSchedule original = slotInDays(doctor, 10);
        Appointment appointment = paidAppointment(patient, original);

        // The doctor moves it to a slot inside the cancellation window.
        DoctorSchedule moved = newSlot(doctor, LocalDateTime.now().plusHours(2));
        appointmentService.reschedule(appointment.getId(), moved.getId(),
                Appointment.ActorRole.DOCTOR, null, doctor.getId());

        appointmentService.cancelAsPatient(
                patient.getId(), appointment.getId(), "the new time does not work");

        assertThat(refundPercentFor(appointment.getId()))
                .as("cutoff must be waived when the doctor moved the appointment")
                .isEqualTo(100);
    }

    /**
     * The waiver is not a permanent flag on the booking. Once the patient moves
     * it themselves they chose the latest time, so the normal cutoff returns -
     * otherwise one doctor reschedule would buy free late cancellation forever.
     */
    @Test
    @DisplayName("patient reschedules after the doctor did: the waiver is given back")
    void patientRescheduleRestoresTheCutoff() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();

        Appointment appointment = paidAppointment(patient, slotInDays(doctor, 10));

        appointmentService.reschedule(appointment.getId(), slotInDays(doctor, 9).getId(),
                Appointment.ActorRole.DOCTOR, null, doctor.getId());

        // Now the patient moves it again, to a time inside the window.
        int cutoff = settings.freeCancellationHours();
        DoctorSchedule chosen = newSlot(doctor,
                LocalDateTime.now().plusHours(Math.max(1, cutoff - 1)));
        appointmentService.reschedule(appointment.getId(), chosen.getId(),
                Appointment.ActorRole.PATIENT, patient.getId(), null);

        appointmentService.cancelAsPatient(patient.getId(), appointment.getId(), "late change");

        assertThat(refundPercentFor(appointment.getId()))
                .as("patient chose this time, so the normal penalty applies again")
                .isEqualTo(settings.partialRefundPercent());
    }

    private int refundPercentFor(Integer appointmentId) {
        var cancelled = events.stream(AppointmentService.AppointmentCancelledEvent.class)
                .filter(e -> e.appointmentId().equals(appointmentId))
                .toList();

        assertThat(cancelled)
                .as("exactly one cancellation event for appointment %s", appointmentId)
                .hasSize(1);

        return cancelled.getFirst().refundPercent();
    }
}
