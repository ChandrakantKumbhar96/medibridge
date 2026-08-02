package com.medibridge.opinion;

import com.medibridge.appointment.dto.BookAppointmentRequest;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.common.enums.ConsultType;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ConflictException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.opinion.dto.OpinionRequest;
import com.medibridge.opinion.dto.OpinionResponse;
import com.medibridge.patient.entity.Patient;
import com.medibridge.record.RecordService;
import com.medibridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Second opinion as a consultation type rather than a label.
 *
 * <p>Three promises the page makes, each asserted here: the specialist has
 * something to review, the price reflects the extra work, and what the patient
 * receives is an opinion document - not a prescription.
 */
class SecondOpinionTest extends AbstractIntegrationTest {

    @Autowired OpinionService opinionService;
    @Autowired RecordService recordService;

    /**
     * The gate that makes the feature honest. "Share your reports" was previously
     * advisory text with nothing behind it, so a patient could pay a premium for
     * a review of a case the specialist could not see.
     */
    @Test
    @DisplayName("a second opinion cannot be booked with nothing to review")
    void bookingRequiresAReport() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();
        DoctorSchedule slot = slotInDays(doctor, 2);

        assertThatThrownBy(() -> appointmentService.book(patient.getId(),
                new BookAppointmentRequest(doctor.getId(), slot.getId(),
                        ConsultType.SECOND_OPINION, "Please review my ECG", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("upload at least");
    }

    @Test
    @DisplayName("with a report on file the booking goes through")
    void bookingSucceedsOnceAReportExists() {
        Doctor doctor = newDoctor();
        Patient patient = patientWithReport();
        DoctorSchedule slot = slotInDays(doctor, 2);

        var booked = appointmentService.book(patient.getId(),
                new BookAppointmentRequest(doctor.getId(), slot.getId(),
                        ConsultType.SECOND_OPINION, "Please review my ECG", null));

        assertThat(booked.type()).isEqualTo(ConsultType.SECOND_OPINION);
    }

    /** An ordinary consultation is untouched by the requirement. */
    @Test
    @DisplayName("a normal consultation still needs no reports")
    void ordinaryConsultationIsUnaffected() {
        Doctor doctor = newDoctor();
        Patient patient = newPatient();
        DoctorSchedule slot = slotInDays(doctor, 2);

        var booked = appointmentService.book(patient.getId(),
                new BookAppointmentRequest(doctor.getId(), slot.getId(),
                        ConsultType.CONSULTATION, "Fever", null));

        assertThat(booked.appointmentId()).isNotNull();
    }

    /**
     * Reviewing another clinician's case file is more work, and the premium is
     * snapshotted at booking - reading it later would let the rate change under
     * a patient who has already paid.
     */
    @Test
    @DisplayName("the second-opinion premium is applied and snapshotted")
    void premiumIsCharged() {
        Doctor doctor = newDoctor(new BigDecimal("800.00"));
        Patient patient = patientWithReport();

        Appointment opinion = appointmentRepository.findById(
                appointmentService.book(patient.getId(), new BookAppointmentRequest(
                        doctor.getId(), slotInDays(doctor, 2).getId(),
                        ConsultType.SECOND_OPINION, "Review", null)).appointmentId())
                .orElseThrow();

        // 150% of 800.00
        assertThat(opinion.getBookedFee()).isEqualByComparingTo(new BigDecimal("1200.00"));

        Appointment normal = appointmentRepository.findById(
                appointmentService.book(newPatient().getId(), new BookAppointmentRequest(
                        doctor.getId(), slotInDays(doctor, 3).getId(),
                        ConsultType.CONSULTATION, "Checkup", null)).appointmentId())
                .orElseThrow();

        assertThat(normal.getBookedFee()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    @DisplayName("issuing an opinion completes the appointment and yields a verdict")
    void opinionIsIssued() {
        Appointment appointment = pastSecondOpinion();

        OpinionResponse issued = opinionService.create(
                appointment.getDoctor().getId(), request(appointment.getId(), false));

        assertThat(issued.agreesWithOriginal()).isFalse();
        assertThat(issued.verdict()).isEqualTo("Differs from the original diagnosis");
        assertThat(issued.pdfUrl()).isEqualTo("/opinions/" + issued.opinionId() + "/pdf");

        assertThat(appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus())
                .as("issuing the opinion is what closes the consultation")
                .isEqualTo(AppointmentStatus.COMPLETED);
    }

    /**
     * The wrong form on the wrong consultation. Without this a specialist could
     * file an opinion against an ordinary consultation, and the patient would be
     * handed a document their doctor never intended to write.
     */
    @Test
    @DisplayName("an opinion cannot be filed against an ordinary consultation")
    void wrongConsultTypeIsRejected() {
        Appointment ordinary = pastAppointment(ConsultType.CONSULTATION);

        assertThatThrownBy(() -> opinionService.create(
                ordinary.getDoctor().getId(), request(ordinary.getId(), true)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a second opinion");
    }

    @Test
    @DisplayName("one appointment yields exactly one opinion")
    void opinionIsIssuedOnlyOnce() {
        Appointment appointment = pastSecondOpinion();
        String doctorId = appointment.getDoctor().getId();

        opinionService.create(doctorId, request(appointment.getId(), true));

        assertThatThrownBy(() -> opinionService.create(
                doctorId, request(appointment.getId(), true)))
                .isInstanceOf(ConflictException.class);
    }

    /** Same ownership rule as everywhere else: a miss is 404, never 403. */
    @Test
    @DisplayName("another doctor cannot issue or read this opinion")
    void ownershipIsEnforced() {
        Appointment appointment = pastSecondOpinion();
        Doctor stranger = newDoctor();

        assertThatThrownBy(() -> opinionService.create(
                stranger.getId(), request(appointment.getId(), true)))
                .isInstanceOf(ResourceNotFoundException.class);

        OpinionResponse issued = opinionService.create(
                appointment.getDoctor().getId(), request(appointment.getId(), true));

        assertThatThrownBy(() ->
                opinionService.pdfForPatient(newPatient().getId(), issued.opinionId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /** The deliverable is a real document, and it is not the prescription one. */
    @Test
    @DisplayName("the opinion renders as a PDF")
    void pdfIsGenerated() {
        Appointment appointment = pastSecondOpinion();
        OpinionResponse issued = opinionService.create(
                appointment.getDoctor().getId(), request(appointment.getId(), true));

        byte[] pdf = opinionService.pdfForPatient(
                appointment.getPatient().getId(), issued.opinionId());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF-");
    }

    // ---------------------------------------------------------------- setup

    private OpinionRequest request(Integer appointmentId, boolean agrees) {
        return new OpinionRequest(
                appointmentId,
                "Grade II ACL tear; surgery advised.",
                "Reviewed the MRI and the operating notes. The ligament is in continuity.",
                agrees,
                "A 12-week supervised physiotherapy programme before considering surgery.",
                "Repeat MRI after 8 weeks.");
    }

    /** A patient who has uploaded something, so a second opinion may be booked. */
    private Patient patientWithReport() {
        Patient patient = newPatient();
        recordService.upload(patient.getId(), null,
                new MockMultipartFile("file", "ecg.pdf", "application/pdf",
                        "%PDF-1.4 fake".getBytes()),
                "ECG Report", "Lab Report");
        return patient;
    }

    private Appointment pastSecondOpinion() {
        return pastAppointment(ConsultType.SECOND_OPINION);
    }

    /**
     * A paid appointment whose time has passed. Booking refuses a past slot, so
     * it is booked ahead and then moved - the same approach the rest of the
     * suite uses, and for the same reason: no sleeping in tests.
     */
    private Appointment pastAppointment(String consultType) {
        Doctor doctor = newDoctor();
        Patient patient = patientWithReport();
        DoctorSchedule slot = slotInDays(doctor, 2);

        var booked = appointmentService.book(patient.getId(), new BookAppointmentRequest(
                doctor.getId(), slot.getId(), consultType, "Please review my case", null));
        appointmentService.markPaidAndConfirm(booked.appointmentId());

        Appointment appointment = appointmentRepository.findById(booked.appointmentId())
                .orElseThrow();
        appointment.setAppointmentDate(LocalDateTime.now().minusHours(1));
        return appointmentRepository.save(appointment);
    }
}
