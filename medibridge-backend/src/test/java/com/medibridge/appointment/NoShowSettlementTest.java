package com.medibridge.appointment;

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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who pays when nobody turns up.
 *
 * <p>Four attendance combinations, and the money rule is deliberately
 * asymmetric: the patient forfeits only where the doctor demonstrably attended.
 * If the platform cannot show the doctor was there, it does not get to bill the
 * patient for the empty room.
 */
@RecordApplicationEvents
class NoShowSettlementTest extends AbstractIntegrationTest {

    @Autowired ApplicationEvents events;

    @Test
    @DisplayName("doctor attended, patient did not: no refund, doctor still paid")
    void patientNoShowForfeits() {
        Appointment appointment = onlyDoctorAttended();

        assertThat(appointmentService.settleNoShow(appointment.getId())).isEqualTo(1);

        Appointment settled = reload(appointment);
        assertThat(settled.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(settled.getNoShowBy()).isEqualTo(Appointment.NoShowParty.PATIENT);
        assertThat(settled.getNoShowMarkedAt()).isNotNull();

        assertThat(noShowRefundEvents(appointment.getId()))
                .as("a patient no-show must not refund")
                .isEmpty();
        assertThat(events.stream(AppointmentService.AppointmentCompletedEvent.class)
                .anyMatch(e -> e.appointmentId().equals(appointment.getId())))
                .as("the doctor held the hour, so the earning is still accrued")
                .isTrue();
    }

    @Test
    @DisplayName("patient attended, doctor did not: full refund")
    void doctorNoShowRefundsInFull() {
        Appointment appointment = onlyPatientAttended();

        assertThat(appointmentService.settleNoShow(appointment.getId())).isEqualTo(1);

        Appointment settled = reload(appointment);
        assertThat(settled.getNoShowBy()).isEqualTo(Appointment.NoShowParty.DOCTOR);

        var refunds = noShowRefundEvents(appointment.getId());
        assertThat(refunds).hasSize(1);
        assertThat(refunds.getFirst().refundPercent()).isEqualTo(100);
        assertThat(refunds.getFirst().noShowBy()).isEqualTo("DOCTOR");
    }

    /**
     * Neither side opened the room. The platform has no evidence the doctor was
     * ready either, so the benefit of the doubt goes to the patient - blaming
     * them by default would be charging for an absence never demonstrated.
     */
    @Test
    @DisplayName("neither attended: refunded, not blamed on the patient")
    void mutualNoShowRefunds() {
        Appointment appointment = nobodyAttended();

        assertThat(appointmentService.settleNoShow(appointment.getId())).isEqualTo(1);

        assertThat(reload(appointment).getNoShowBy()).isEqualTo(Appointment.NoShowParty.BOTH);

        var refunds = noShowRefundEvents(appointment.getId());
        assertThat(refunds).hasSize(1);
        assertThat(refunds.getFirst().refundPercent()).isEqualTo(100);
    }

    /**
     * The case that protects the existing workflow: a consultation that really
     * happened and is waiting on the doctor's notes must never be swept into a
     * no-show just because it is past its slot time.
     */
    @Test
    @DisplayName("both attended: left alone for the doctor's notes")
    void attendedConsultationIsNotTouched() {
        Appointment appointment = bothAttended();

        assertThat(appointmentService.settleNoShow(appointment.getId()))
                .as("settle must be a no-op")
                .isZero();

        Appointment untouched = reload(appointment);
        assertThat(untouched.getStatus()).isEqualTo(AppointmentStatus.ACCEPTED);
        assertThat(untouched.getNoShowBy()).isNull();
    }

    @Test
    @DisplayName("settling twice does not settle twice")
    void settlementIsIdempotent() {
        Appointment appointment = onlyDoctorAttended();

        assertThat(appointmentService.settleNoShow(appointment.getId())).isEqualTo(1);
        assertThat(appointmentService.settleNoShow(appointment.getId()))
                .as("second sweep of the same row must do nothing")
                .isZero();
    }

    @Test
    @DisplayName("the sweep only picks up rooms that have closed")
    void openRoomsAreNotCandidates() {
        Appointment future = room(false, false, LocalDateTime.now().plusHours(3));

        assertThat(appointmentService.findNoShowCandidateIds())
                .as("an appointment whose room is still open is not judgeable yet")
                .doesNotContain(future.getId());
    }

    // ---------------------------------------------------------------- setup

    // Named rather than a pair of booleans at the call site. closedRoom(true,
    // false) reads identically whichever way round the arguments go, and the
    // first version of this test had them swapped in two places - the compiler
    // cannot catch it and the assertion message just looks like a real bug.

    private Appointment onlyDoctorAttended() {
        return closedRoom(false, true);
    }

    private Appointment onlyPatientAttended() {
        return closedRoom(true, false);
    }

    private Appointment nobodyAttended() {
        return closedRoom(false, false);
    }

    private Appointment bothAttended() {
        return closedRoom(true, true);
    }

    private Appointment closedRoom(boolean patientJoined, boolean doctorJoined) {
        return room(patientJoined, doctorJoined, LocalDateTime.now().minusHours(3));
    }

    /**
     * Produces a paid appointment with a given meeting window and attendance.
     *
     * <p>Booking refuses a slot in the past, so the appointment is created in
     * the future and then moved directly - the alternative is a test that
     * sleeps, which would be slow and still flaky.
     */
    private Appointment room(boolean patientJoined, boolean doctorJoined,
                             LocalDateTime when) {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();
        DoctorSchedule slot = slotInDays(doctor, 2);

        Appointment appointment = paidAppointment(patient, slot);

        appointment.setAppointmentDate(when);
        appointment.setMeetingJoinFrom(when.minusMinutes(15));
        appointment.setMeetingValidUntil(when.plusMinutes(60));
        if (patientJoined) appointment.setPatientJoinedAt(when.plusMinutes(1));
        if (doctorJoined) appointment.setDoctorJoinedAt(when.plusMinutes(2));

        return appointmentRepository.save(appointment);
    }

    private Appointment reload(Appointment a) {
        return appointmentRepository.findById(a.getId()).orElseThrow();
    }

    private java.util.List<AppointmentService.AppointmentNoShowEvent> noShowRefundEvents(
            Integer appointmentId) {
        return events.stream(AppointmentService.AppointmentNoShowEvent.class)
                .filter(e -> e.appointmentId().equals(appointmentId))
                .toList();
    }
}
