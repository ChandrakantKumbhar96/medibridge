package com.medibridge.opinion;

import com.medibridge.appointment.AppointmentMapper;
import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.appointment.AppointmentService;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.common.enums.ConsultType;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ConflictException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.opinion.dto.OpinionRequest;
import com.medibridge.opinion.dto.OpinionResponse;
import com.medibridge.opinion.entity.MedicalOpinion;
import com.medibridge.patient.entity.Patient;
import com.medibridge.pdf.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Second opinions: the reviewing specialist's written judgement, and the
 * document the patient actually receives.
 *
 * <p>This is what makes "Second Opinion" a consultation type rather than a
 * label. The booking flow already routed the patient to an independent
 * specialist and required their reports; this closes the loop by producing a
 * deliverable that is not a prescription.
 */
@Service
@RequiredArgsConstructor
public class OpinionService {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MedicalOpinionRepository opinionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PdfService pdfService;
    private final ApplicationEventPublisher events;

    /**
     * The reviewing specialist issues their opinion.
     *
     * <p>Completes the appointment and publishes the same event a prescription
     * does. Without that the doctor would never be paid for a second opinion —
     * the payout module accrues on {@code AppointmentCompletedEvent}, and this
     * is the only way a second opinion ever reaches COMPLETED.
     */
    @Transactional
    public OpinionResponse create(String doctorId, OpinionRequest request) {
        Appointment appointment = appointmentRepository
                .findByIdAndDoctorId(request.appointmentId(), doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (!ConsultType.isSecondOpinion(appointment.getConsultType())) {
            throw new BadRequestException(
                    "This appointment is a " + appointment.getConsultType()
                            + ", not a second opinion. Use the prescription form instead.");
        }

        if (appointment.getStatus() != AppointmentStatus.ACCEPTED
                && appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BadRequestException(
                    "An opinion can only be issued for a confirmed appointment");
        }

        // Same rule as a prescription: the consultation has to have happened.
        // Reviewing a case file before the booked time would let a specialist
        // close - and be paid for - a consultation that never took place.
        if (!appointment.hasStarted()) {
            throw new BadRequestException(
                    "This consultation is scheduled for "
                            + AppointmentMapper.describe(appointment)
                            + ". An opinion cannot be issued before it takes place.");
        }

        if (opinionRepository.existsByAppointmentId(appointment.getId())) {
            throw new ConflictException("An opinion has already been issued for this appointment");
        }

        MedicalOpinion opinion = opinionRepository.save(MedicalOpinion.builder()
                .appointment(appointment)
                .originalDiagnosis(request.originalDiagnosis())
                .findings(request.findings())
                .agreesWithOriginal(request.agreesWithOriginal())
                .recommendation(request.recommendation())
                .suggestedTests(blankToNull(request.suggestedTests()))
                .issuedAt(LocalDateTime.now())
                .build());

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        events.publishEvent(
                new AppointmentService.AppointmentCompletedEvent(appointment.getId()));

        return toDto(opinion);
    }

    @Transactional(readOnly = true)
    public List<OpinionResponse> listForPatient(Integer patientId) {
        return opinionRepository.findForPatient(patientId).stream().map(this::toDto).toList();
    }

    // ------------------------------------------------------------------ PDF

    @Transactional(readOnly = true)
    public byte[] pdfForPatient(Integer patientId, Integer opinionId) {
        return render(opinionRepository.findByIdAndPatientId(opinionId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion not found")));
    }

    @Transactional(readOnly = true)
    public byte[] pdfForDoctor(String doctorId, Integer opinionId) {
        return render(opinionRepository.findByIdAndDoctorId(opinionId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion not found")));
    }

    /**
     * Renders {@code pdf/opinion}, which is a different template from the
     * prescription entirely - not the same layout with the medicine table
     * hidden. The document has to look like what it is the moment the patient
     * opens it.
     */
    private byte[] render(MedicalOpinion o) {
        Appointment appointment = o.getAppointment();
        Patient patient = appointment.getPatient();
        Doctor doctor = appointment.getDoctor();

        Map<String, Object> model = new HashMap<>();
        model.put("opinionNo", String.format("MB-SO-%05d", o.getId()));
        model.put("issuedOn", o.getIssuedAt().format(DATE));
        model.put("generatedOn", LocalDateTime.now().format(STAMP));

        model.put("patientName", patient.getFullName());
        model.put("patientAge", AppointmentMapper.ageOf(patient.getDateOfBirth()));
        model.put("patientGender", patient.getGender() == null ? null : patient.getGender().name());
        model.put("patientPhone", patient.getPhone());

        model.put("doctorName", doctor.getFullName());
        model.put("doctorSpecialization", doctor.getSpecialization().getName());
        model.put("doctorLicense", doctor.getLicenseNumber());
        model.put("doctorQualifications", doctor.getQualifications());

        model.put("originalDiagnosis", o.getOriginalDiagnosis());
        model.put("findings", o.getFindings());
        model.put("agrees", o.getAgreesWithOriginal());
        model.put("verdict", verdictOf(o.getAgreesWithOriginal()));
        model.put("recommendation", o.getRecommendation());
        model.put("suggestedTests", o.getSuggestedTests());

        return pdfService.render("pdf/opinion", model);
    }

    /**
     * One wording for the verdict, produced here and used by every surface.
     * Rendered independently in the PDF, the patient's list and the doctor's
     * screen, it would drift into three different phrasings - and this is the
     * sentence the patient will quote to their original doctor.
     */
    private String verdictOf(Boolean agrees) {
        return Boolean.TRUE.equals(agrees)
                ? "Agrees with the original diagnosis"
                : "Differs from the original diagnosis";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private OpinionResponse toDto(MedicalOpinion o) {
        Appointment a = o.getAppointment();
        return new OpinionResponse(
                o.getId(),
                a.getId(),
                a.getPatient().getFullName(),
                a.getDoctor().getFullName(),
                a.getDoctor().getSpecialization().getName(),
                o.getOriginalDiagnosis(),
                o.getFindings(),
                o.getAgreesWithOriginal(),
                verdictOf(o.getAgreesWithOriginal()),
                o.getRecommendation(),
                o.getSuggestedTests(),
                o.getIssuedAt().format(DATE),
                "/opinions/" + o.getId() + "/pdf");
    }
}
