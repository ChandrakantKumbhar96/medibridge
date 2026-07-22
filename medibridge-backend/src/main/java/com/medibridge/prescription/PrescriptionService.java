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
                    "A prescription can only be issued for an accepted or completed appointment");
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

        // Issuing a prescription implies the consultation happened.
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

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

        Map<String, Object> model = new HashMap<>();
        model.put("prescriptionNo", String.format("MB-RX-%05d", p.getId()));
        model.put("dateIssued", p.getDateIssued().format(DATE));
        model.put("generatedOn", LocalDateTime.now().format(STAMP));

        model.put("patientName", patient.getFullName());
        model.put("patientAge", AppointmentMapper.ageOf(patient.getDateOfBirth()));
        model.put("patientGender", patient.getGender().name());
        model.put("patientBloodGroup", patient.getBloodGroup());
        model.put("patientPhone", patient.getPhone());

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

    /** Full medical history PDF: consultations, prescriptions, uploaded documents. */
    @Transactional(readOnly = true)
    public byte[] medicalHistoryPdf(Patient patient) {
        List<Appointment> appointments =
                appointmentRepository.findByPatientIdAndStatusInOrderByAppointmentDateDesc(
                        patient.getId(), Arrays.asList(AppointmentStatus.values()));

        List<Prescription> prescriptions =
                prescriptionRepository.findByPatientIdOrderByDateIssuedDesc(patient.getId());

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
                .findByPatientIdOrderByUploadDateDesc(patient.getId()).stream()
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
                p.getPatient().getFullName(),
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
