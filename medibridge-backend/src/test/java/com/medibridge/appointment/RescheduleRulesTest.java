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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reschedule policy, and the asymmetry between the two sides.
 *
 * <p>A patient is limited by notice period and count; a doctor is not, because
 * a doctor with an emergency must always be able to move a slot and the patient
 * is not penalised for it. Both sides keep the same appointment id, which is
 * what lets the payment ride along instead of being refunded and re-charged.
 */
class RescheduleRulesTest extends AbstractIntegrationTest {

    @Autowired SettingsProvider settings;

    @Test
    @DisplayName("the appointment id survives, so the payment carries over")
    void rescheduleKeepsTheSameAppointment() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();

        Appointment appointment = paidAppointment(patient, slotInDays(doctor, 10));
        Integer id = appointment.getId();
        var originalFee = appointment.getTotalAmount();
        String originalLink = appointment.getMeetingLink();

        DoctorSchedule target = slotInDays(doctor, 11);
        appointmentService.reschedule(id, target.getId(),
                Appointment.ActorRole.PATIENT, patient.getId(), null);

        Appointment moved = appointmentRepository.findById(id).orElseThrow();
        assertThat(moved.getId()).isEqualTo(id);
        assertThat(moved.getTotalAmount()).isEqualByComparingTo(originalFee);
        assertThat(moved.getMeetingLink())
                .as("the patient may have saved this URL; reissuing would break it")
                .isEqualTo(originalLink);
        assertThat(moved.getStatus())
                .as("still confirmed, so it can be moved again")
                .isEqualTo(AppointmentStatus.ACCEPTED);
        assertThat(moved.getRescheduleCount()).isEqualTo((short) 1);
        assertThat(moved.getOriginalDate())
                .as("the first booked date is preserved")
                .isNotNull();
    }

    @Test
    @DisplayName("the old slot is released and the new one claimed")
    void slotsAreSwapped() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();

        DoctorSchedule original = slotInDays(doctor, 10);
        Appointment appointment = paidAppointment(patient, original);
        DoctorSchedule target = slotInDays(doctor, 12);

        appointmentService.reschedule(appointment.getId(), target.getId(),
                Appointment.ActorRole.PATIENT, patient.getId(), null);

        assertThat(scheduleRepository.findById(original.getId()).orElseThrow().getIsBooked())
                .as("the vacated slot must be bookable again")
                .isFalse();
        assertThat(scheduleRepository.findById(target.getId()).orElseThrow().getIsBooked())
                .isTrue();
    }

    @Test
    @DisplayName("patient inside the notice period is refused")
    void patientCannotRescheduleTooLate() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();

        int minHours = settings.getInt("reschedule_min_hours", 4);
        DoctorSchedule imminent = newSlot(doctor,
                LocalDateTime.now().plusHours(Math.max(1, minHours - 1)));
        Appointment appointment = paidAppointment(patient, imminent);

        DoctorSchedule target = slotInDays(doctor, 5);

        assertThatThrownBy(() -> appointmentService.reschedule(appointment.getId(),
                target.getId(), Appointment.ActorRole.PATIENT, patient.getId(), null))
                .hasMessageContaining("in advance");
    }

    @Test
    @DisplayName("the doctor is not bound by the notice period")
    void doctorMayMoveAnImminentSlot() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();

        int minHours = settings.getInt("reschedule_min_hours", 4);
        DoctorSchedule imminent = newSlot(doctor,
                LocalDateTime.now().plusHours(Math.max(1, minHours - 1)));
        Appointment appointment = paidAppointment(patient, imminent);

        DoctorSchedule target = slotInDays(doctor, 5);
        appointmentService.reschedule(appointment.getId(), target.getId(),
                Appointment.ActorRole.DOCTOR, null, doctor.getId());

        Appointment moved = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(moved.getRescheduledBy()).isEqualTo(Appointment.ActorRole.DOCTOR);
        assertThat(moved.getSchedule().getId()).isEqualTo(target.getId());
    }

    @Test
    @DisplayName("the patient's reschedule count is capped")
    void patientRescheduleCountIsCapped() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();

        Appointment appointment = paidAppointment(patient, slotInDays(doctor, 20));
        int max = settings.getInt("max_reschedules", 2);

        for (int i = 0; i < max; i++) {
            appointmentService.reschedule(appointment.getId(),
                    slotInDays(doctor, 21 + i).getId(),
                    Appointment.ActorRole.PATIENT, patient.getId(), null);
        }

        DoctorSchedule oneTooMany = slotInDays(doctor, 40);
        assertThatThrownBy(() -> appointmentService.reschedule(appointment.getId(),
                oneTooMany.getId(), Appointment.ActorRole.PATIENT, patient.getId(), null))
                .hasMessageContaining("already been rescheduled");
    }

    @Test
    @DisplayName("moving to another doctor is refused: that is a different consultation")
    void crossDoctorMoveIsRefused() {
        Doctor doctor = newDoctor();
        Doctor other = newDoctor();
        Patient patient = newPatient();

        Appointment appointment = paidAppointment(patient, slotInDays(doctor, 10));
        DoctorSchedule foreign = slotInDays(other, 11);

        assertThatThrownBy(() -> appointmentService.reschedule(appointment.getId(),
                foreign.getId(), Appointment.ActorRole.PATIENT, patient.getId(), null))
                .hasMessageContaining("different doctor");
    }

    @Test
    @DisplayName("an unpaid hold cannot be rescheduled")
    void onlyConfirmedAppointmentsCanMove() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();

        // Booked but never paid, so it is still PENDING_PAYMENT.
        var pending = appointmentService.book(patient.getId(),
                new com.medibridge.appointment.dto.BookAppointmentRequest(
                        doctor.getId(), slotInDays(doctor, 10).getId(), "Consultation", null, null));

        DoctorSchedule target = slotInDays(doctor, 11);

        assertThatThrownBy(() -> appointmentService.reschedule(pending.appointmentId(),
                target.getId(), Appointment.ActorRole.PATIENT, patient.getId(), null))
                .hasMessageContaining("confirmed");
    }

    /**
     * Ownership is enforced by the {@code (id, ownerId)} lookup, and a miss is
     * a 404 rather than a 403 - telling a stranger that an appointment exists
     * is itself a disclosure.
     */
    @Test
    @DisplayName("another patient cannot reschedule someone else's appointment")
    void reschedulingIsOwnershipScoped() {
        Doctor doctor = newDoctor();
        Patient owner = newPatient();
        Patient stranger = newPatient();

        Appointment appointment = paidAppointment(owner, slotInDays(doctor, 10));
        DoctorSchedule target = slotInDays(doctor, 11);

        assertThatThrownBy(() -> appointmentService.reschedule(appointment.getId(),
                target.getId(), Appointment.ActorRole.PATIENT, stranger.getId(), null))
                .hasMessageContaining("not found");
    }
}
