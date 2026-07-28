package com.medibridge.admin;

import com.medibridge.common.enums.AccountStatus;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.patient.entity.Patient;
import com.medibridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Taking a doctor offline must not strand paid patients.
 *
 * <p>Flipping the account flag hides the doctor from search, but it does not
 * undo appointments people have already been charged for. Without this guard
 * those patients keep a booking with a doctor the platform has just told to
 * stop working, and nobody is notified.
 */
class DoctorDeactivationGuardTest extends AbstractIntegrationTest {

    @Autowired AdminService adminService;

    @Test
    @DisplayName("cannot suspend a doctor who owes confirmed appointments")
    void suspendIsBlockedWhileCommitmentsExist() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();
        paidAppointment(patient, slotInDays(doctor, 5));

        assertThatThrownBy(() ->
                adminService.setDoctorStatus(doctor.getId(), "suspended"))
                .hasMessageContaining("already paid for");

        assertThat(doctorRepository.findById(doctor.getId()).orElseThrow().getStatus())
                .as("the status must not have changed")
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("cannot deactivate either")
    void deactivateIsBlockedToo() {
        Doctor doctor = newDoctor();
        paidAppointment(newPatient(), slotInDays(doctor, 5));

        assertThatThrownBy(() ->
                adminService.setDoctorStatus(doctor.getId(), "inactive"))
                .hasMessageContaining("already paid for");
    }

    @Test
    @DisplayName("allowed once the queue is cleared")
    void deactivateSucceedsWhenNothingIsOwed() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();
        var appointment = paidAppointment(patient, slotInDays(doctor, 5));

        // The documented way out: cancel, which refunds the patient in full.
        appointmentService.cancelAsDoctor(doctor.getId(), appointment.getId(), "closing practice");

        adminService.setDoctorStatus(doctor.getId(), "inactive");

        assertThat(doctorRepository.findById(doctor.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.INACTIVE);
    }

    @Test
    @DisplayName("past appointments do not block: only future commitments count")
    void onlyFutureAppointmentsBlock() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();

        var appointment = paidAppointment(patient, slotInDays(doctor, 5));
        var entity = appointmentRepository.findById(appointment.getId()).orElseThrow();
        entity.setAppointmentDate(java.time.LocalDateTime.now().minusDays(2));
        appointmentRepository.save(entity);

        adminService.setDoctorStatus(doctor.getId(), "inactive");

        assertThat(doctorRepository.findById(doctor.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.INACTIVE);
    }

    @Test
    @DisplayName("approving a doctor is never blocked")
    void activationIsAlwaysAllowed() {
        Doctor doctor = newDoctor();
        paidAppointment(newPatient(), slotInDays(doctor, 5));

        adminService.setDoctorStatus(doctor.getId(), "active");

        assertThat(doctorRepository.findById(doctor.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
    }
}
