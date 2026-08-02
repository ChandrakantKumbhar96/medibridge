package com.medibridge.prescription;

import com.medibridge.appointment.AppointmentMapper;
import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ConflictException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.pdf.PdfService;
import com.medibridge.prescription.dto.PrescriptionRequest;
import com.medibridge.prescription.dto.PrescriptionResponse;
import com.medibridge.prescription.dto.PrescriptionStatusResponse;
import com.medibridge.prescription.entity.ConsultationRecord;
import com.medibridge.prescription.entity.Prescription;
import com.medibridge.prescription.entity.PrescriptionItem;
import com.medibridge.patient.entity.Patient;
import com.medibridge.record.FileStorageService;
import com.medibridge.record.MedicalReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalReportRepository reportRepository;
    private final PdfService pdfService;
    private final com.medibridge.notification.NotificationService notificationService;
    private final org.springframework.context.ApplicationEventPublisher events;

    /** Backs the chat-service's get_prescription_status tool - see spring_client.py. */
    @Transactional(readOnly = true)
    public PrescriptionStatusResponse latestForPatient(Integer patientId) {
        Prescription rx = prescriptionRepository.findByPatientIdOrderByDateIssuedDesc(patientId)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No prescription found"));
        return new PrescriptionStatusResponse(rx.getId(), rx.getDateIssued().format(DATE), rx.getDoctor().getFullName());
    }

    /**
     * Doctor records the consultation and issues the prescription. Both writes
     * are in one transaction - a prescription with no consultation behind it
     * would be a clinical record with no context.
     */
    @Transactional
    public PrescriptionResponse create(String doctorId, PrescriptionRequest request) {
        Appointment appointment = appointmentRepository
                .findByIdAndDoctorId(request.appointmentId(), doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.ACCEPTED
                && appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BadRequestException(
                    "A prescription can only be issued for a confirmed appointment");
        }

        // A prescription is a record of a consultation that happened. Without
        // this check a doctor could write up - and thereby complete - a
        // consultation scheduled for next month, which would also unlock a
        // patient review for a visit that never took place.
        if (!appointment.hasStarted()) {
            throw new BadRequestException(
                    "This consultation is scheduled for "
                            + AppointmentMapper.describe(appointment)
                            + ". A prescription cannot be issued before it takes place.");
        }

        if (consultationRepository.existsByAppointmentId(appointment.getId())) {
            throw new ConflictException("This appointment already has a consultation record");
        }

        ConsultationRecord consultation = consultationRepository.save(ConsultationRecord.builder()
                .appointment(appointment)
                .diagnosis(request.diagnosis())
                .notes(request.notes())
                .followUpDate(request.followUpDate())
                .build());

        Prescription prescription = Prescription.builder()
                .consultation(consultation)
                .patient(appointment.getPatient())
                .doctor(appointment.getDoctor())
                .dateIssued(LocalDate.now())
                .advice(request.advice())
                .items(new ArrayList<>())
                .build();

        int order = 0;
        for (PrescriptionRequest.Item item : request.medicines()) {
            prescription.addItem(PrescriptionItem.builder()
                    .medicineName(item.medicineName())
                    .dosage(item.dosage())
                    .frequency(item.frequency())
                    .duration(item.duration())
                    .instructions(item.instructions())
                    .sortOrder(order++)
                    .build());
        }

        prescription = prescriptionRepository.save(prescription);

        // Issuing a prescription implies the consultation happened. This is the
        // second path to COMPLETED (the other is the doctor pressing Complete),
        // so it must publish the same event or earnings would be missed for
        // every consultation that ended with a prescription.
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        events.publishEvent(
                new com.medibridge.appointment.AppointmentService
                        .AppointmentCompletedEvent(appointment.getId()));

        notificationService.sendPrescriptionReady(appointment);

        return toDto(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> listForPatient(Integer patientId) {
        return prescriptionRepository.findByPatientIdOrderByDateIssuedDesc(patientId)
                .stream().map(this::toDto).toList();
    }

    // ------------------------------------------------------------------ PDF

    /** Ownership is enforced by the (id, ownerId) lookup, same as everywhere else. */
    @Transactional(readOnly = true)
    public byte[] prescriptionPdfForPatient(Integer patientId, Integer prescriptionId) {
        return renderPrescription(prescriptionRepository
                .findByIdAndPatientId(prescriptionId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found")));
    }

    @Transactional(readOnly = true)
    public byte[] prescriptionPdfForDoctor(String doctorId, Integer prescriptionId) {
        return renderPrescription(prescriptionRepository
                .findByIdAndDoctorId(prescriptionId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found")));
    }

    private byte[] renderPrescription(Prescription p) {
        Patient patient = p.getPatient();
        // Null for a self-booking. Everything clinical on the sheet comes from
        // the subject, because the dose printed on it is computed from their age
        // and weight band - printing the parent's would be a real dispensing
        // error, not a cosmetic one.
        var subject = p.getConsultation().getAppointment().getFamilyMember();

        Map<String, Object> model = new HashMap<>();
        model.put("prescriptionNo", String.format("MB-RX-%05d", p.getId()));
        model.put("dateIssued", p.getDateIssued().format(DATE));
        model.put("generatedOn", LocalDateTime.now().format(STAMP));

        model.put("patientName", p.getConsultation().getAppointment().subjectName());
        model.put("patientAge", AppointmentMapper.ageOf(
                p.getConsultation().getAppointment().subjectDateOfBirth()));
        model.put("patientGender", subject != null
                ? subject.getGender().name()
                : patient.getGender() == null ? null : patient.getGender().name());
        model.put("patientBloodGroup", subject != null
                ? subject.getBloodGroup() : patient.getBloodGroup());
        // Contact details stay the account holder's - the child has no phone,
        // and the pharmacy needs to reach whoever is responsible.
        model.put("patientPhone", patient.getPhone());
        model.put("accountHolderName", subject == null ? null : patient.getFullName());
        model.put("subjectRelation", subject == null ? null : subject.getRelation().name());

        model.put("doctorName", p.getDoctor().getFullName());
        model.put("doctorSpecialization", p.getDoctor().getSpecialization().getName());
        model.put("doctorLicense", p.getDoctor().getLicenseNumber());
        model.put("doctorPhone", p.getDoctor().getPhone());

        model.put("diagnosis", p.getConsultation().getDiagnosis());
        model.put("notes", p.getConsultation().getNotes());
        model.put("advice", p.getAdvice());
        model.put("followUpDate", p.getConsultation().getFollowUpDate() == null
                ? null : p.getConsultation().getFollowUpDate().format(DATE));

        model.put("medicines", p.getItems().stream()
                .map(i -> new PrescriptionResponse.Item(
                        i.getMedicineName(), i.getDosage(), i.getFrequency(),
                        i.getDuration(), i.getInstructions()))
                .toList());

        return pdfService.render("pdf/prescription", model);
    }

    /**
     * Full medical history PDF: consultations, prescriptions, uploaded documents.
     *
     * <p>The account holder's own history only. Everything below is filtered to
     * visits and documents with no dependent attached, because the sheet is
     * headed with this patient's name, age and blood group - folding a child's
     * diagnoses into a document titled with the parent's name is how a history
     * PDF becomes actively misleading to whoever reads it next.
     */
    @Transactional(readOnly = true)
    public byte[] medicalHistoryPdf(Patient patient) {
        List<Appointment> appointments =
                appointmentRepository.findByPatientIdAndStatusInOrderByAppointmentDateDesc(
                                patient.getId(), Arrays.asList(AppointmentStatus.values()))
                        .stream().filter(a -> !a.isForDependent()).toList();

        List<Prescription> prescriptions =
                prescriptionRepository.findByPatientIdOrderByDateIssuedDesc(patient.getId())
                        .stream()
                        .filter(p -> !p.getConsultation().getAppointment().isForDependent())
                        .toList();

        List<Map<String, String>> consultations = appointments.stream()
                .map(a -> consultationRepository.findByAppointmentId(a.getId()).orElse(null))
                .filter(Objects::nonNull)
                .map(c -> Map.of(
                        "date", c.getAppointment().getAppointmentDate().format(DATE),
                        "doctor", c.getAppointment().getDoctor().getFullName(),
                        "specialization",
                            c.getAppointment().getDoctor().getSpecialization().getName(),
                        "diagnosis", c.getDiagnosis()))
                .toList();

        List<Map<String, String>> prescriptionRows = prescriptions.stream()
                .map(p -> Map.of(
                        "date", p.getDateIssued().format(DATE),
                        "doctor", p.getDoctor().getFullName(),
                        "medicines", p.getItems().stream()
                                .map(i -> i.getMedicineName() + " " + i.getDosage())
                                .reduce((a, b) -> a + ", " + b).orElse("-")))
                .toList();

        List<Map<String, String>> reportRows = reportRepository
                .findByPatientIdAndFamilyMemberIsNullOrderByUploadDateDesc(patient.getId()).stream()
                .map(r -> Map.of(
                        "date", r.getUploadDate() == null ? "-" : r.getUploadDate().format(DATE),
                        "name", r.getReportName(),
                        "type", r.getReportType(),
                        "size", FileStorageService.humanSize(
                                r.getFileSizeBytes() == null ? 0 : r.getFileSizeBytes())))
                .toList();

        Map<String, Object> model = new HashMap<>();
        model.put("reportNo", String.format("MB-MH-%05d", patient.getId()));
        model.put("generatedOn", LocalDateTime.now().format(STAMP));
        model.put("patientName", patient.getFullName());
        model.put("patientAge", AppointmentMapper.ageOf(patient.getDateOfBirth()));
        model.put("patientGender", patient.getGender().name());
        model.put("patientBloodGroup", patient.getBloodGroup());
        model.put("patientPhone", patient.getPhone());
        model.put("patientEmail", patient.getEmail());

        model.put("totalAppointments", appointments.size());
        model.put("completedConsults", consultations.size());
        model.put("totalPrescriptions", prescriptions.size());
        model.put("totalReports", reportRows.size());

        model.put("consultations", consultations);
        model.put("prescriptions", prescriptionRows);
        model.put("reports", reportRows);

        return pdfService.render("pdf/medical-report", model);
    }

    private PrescriptionResponse toDto(Prescription p) {
        return new PrescriptionResponse(
                p.getId(),
                p.getConsultation().getAppointment().getId(),
                // The subject of the visit, not the account holder - derived from
                // the appointment rather than stored again on the prescription,
                // so there is no second copy to drift.
                p.getConsultation().getAppointment().subjectName(),
                p.getDoctor().getFullName(),
                p.getDoctor().getSpecialization().getName(),
                p.getConsultation().getDiagnosis(),
                p.getConsultation().getNotes(),
                p.getAdvice(),
                p.getDateIssued().format(DATE),
                p.getConsultation().getFollowUpDate() == null
                        ? null : p.getConsultation().getFollowUpDate().format(DATE),
                p.getItems().stream()
                        .map(i -> new PrescriptionResponse.Item(
                                i.getMedicineName(), i.getDosage(), i.getFrequency(),
                                i.getDuration(), i.getInstructions()))
                        .toList(),
                "/prescriptions/" + p.getId() + "/pdf");
    }
}
