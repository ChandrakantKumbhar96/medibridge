package com.medibridge.record;

import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.patient.FamilyMemberService;
import com.medibridge.patient.PatientRepository;
import com.medibridge.patient.entity.FamilyMember;
import com.medibridge.patient.entity.Patient;
import com.medibridge.record.dto.RecordResponse;
import com.medibridge.record.entity.MedicalReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecordService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** {@code ?subject=self} - the account holder's own documents only. */
    public static final String SUBJECT_SELF = "self";

    private final MedicalReportRepository reportRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final FamilyMemberService familyMemberService;
    private final FileStorageService storage;

    /**
     * Everything on the account - the holder's documents and every dependent's.
     *
     * <p>One list rather than one call per person: they all belong to the same
     * owner, and the response carries {@code family_member_id} so the UI can
     * label and group them.
     */
    @Transactional(readOnly = true)
    public List<RecordResponse> listForPatient(Integer patientId) {
        return reportRepository.findByPatientIdOrderByUploadDateDesc(patientId)
                .stream().map(this::toDto).toList();
    }

    /**
     * @param subject null for everyone on the account, {@value #SUBJECT_SELF} for
     *                the holder alone, or a dependent's id.
     *
     * <p>The dependent id is resolved through {@code requireOwned} rather than
     * passed straight into the query, so an id belonging to another account is a
     * 404 instead of an empty list - an empty list would still confirm the id
     * parsed, and a caller could tell "not yours" from "no documents".
     */
    @Transactional(readOnly = true)
    public List<RecordResponse> listForSubject(Integer patientId, String subject) {
        if (subject == null || subject.isBlank()) {
            return listForPatient(patientId);
        }
        if (SUBJECT_SELF.equalsIgnoreCase(subject)) {
            return reportRepository
                    .findByPatientIdAndFamilyMemberIsNullOrderByUploadDateDesc(patientId)
                    .stream().map(this::toDto).toList();
        }

        Integer familyMemberId;
        try {
            familyMemberId = Integer.valueOf(subject.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("subject must be 'self' or a family member id");
        }
        familyMemberService.requireOwned(patientId, familyMemberId);

        return reportRepository
                .findByPatientIdAndFamilyMemberIdOrderByUploadDateDesc(patientId, familyMemberId)
                .stream().map(this::toDto).toList();
    }

    /**
     * How many documents are on file for one person.
     *
     * <p>Exists so other modules can ask without touching
     * {@code MedicalReportRepository} - the appointment module needs this to
     * refuse a second opinion with nothing to review, and reaching into another
     * module's repository is exactly the coupling the package layout is meant to
     * prevent.
     *
     * @param familyMemberId null to count the account holder's own documents.
     *                       Counting the whole account instead would let a parent
     *                       book a second opinion for a child using their own
     *                       reports, which is precisely the case the rule exists
     *                       to stop.
     */
    @Transactional(readOnly = true)
    public long countForSubject(Integer patientId, Integer familyMemberId) {
        return familyMemberId == null
                ? reportRepository.countByPatientIdAndFamilyMemberIsNull(patientId)
                : reportRepository.countByPatientIdAndFamilyMemberId(patientId, familyMemberId);
    }

    @Transactional
    public RecordResponse upload(Integer patientId, Integer familyMemberId, MultipartFile file,
                                 String reportName, String reportType) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        // Null for the holder's own document; anything else is proven to belong
        // to this account before a file is written.
        FamilyMember subject = familyMemberService.requireOwned(patientId, familyMemberId);

        if (reportName == null || reportName.isBlank()) {
            throw new BadRequestException("Report name is required");
        }

        String storedName = storage.store(file);

        MedicalReport report = MedicalReport.builder()
                .patient(patient)
                .familyMember(subject)
                .reportName(reportName.trim())
                .reportType(reportType == null || reportType.isBlank() ? "Document" : reportType)
                .reportDataUrl(storedName)
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .uploadedByType(MedicalReport.UploaderType.PATIENT)
                .uploadedById(String.valueOf(patientId))
                .build();

        return toDto(reportRepository.save(report));
    }

    /**
     * Patient downloading a report from their own account.
     *
     * <p>Queried by (id, patientId) rather than id alone, so changing the id in
     * the URL yields 404 rather than someone else's file. 404 not 403 - a 403
     * would confirm the report exists. The owner may read their dependents'
     * documents, which is the whole point of the account.
     */
    @Transactional(readOnly = true)
    public DownloadedFile downloadAsPatient(Integer patientId, Integer reportId) {
        MedicalReport report = reportRepository.findByIdAndPatientId(reportId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return readFile(report);
    }

    /**
     * A doctor may read a report only for someone they have actually treated.
     * Holding ROLE_DOCTOR is not by itself sufficient.
     *
     * <p>Treating the parent does not grant the child's file: the match is on the
     * subject of the appointment, not on the account it was booked from.
     */
    @Transactional(readOnly = true)
    public DownloadedFile downloadAsDoctor(String doctorId, Integer reportId) {
        MedicalReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (!treatedSubjects(doctorId).contains(subjectKey(
                report.getPatient().getId(),
                report.getFamilyMember() == null ? null : report.getFamilyMember().getId()))) {
            throw new ResourceNotFoundException("Report not found");
        }
        return readFile(report);
    }

    /**
     * The doctor portal's view of one patient's documents.
     *
     * <p>Narrowed to the subjects this doctor has treated on that account. A
     * paediatrician who has only ever seen the child has no business reading the
     * parent's cardiology reports, even though both live under one login.
     */
    @Transactional(readOnly = true)
    public List<RecordResponse> listForDoctorsPatient(String doctorId, Integer patientId) {
        Set<String> treated = treatedSubjects(doctorId);

        List<RecordResponse> visible = reportRepository
                .findByPatientIdOrderByUploadDateDesc(patientId).stream()
                .filter(r -> treated.contains(subjectKey(
                        r.getPatient().getId(),
                        r.getFamilyMember() == null ? null : r.getFamilyMember().getId())))
                .map(this::toDto)
                .toList();

        // No treating relationship with anyone on this account at all - answer as
        // if the account did not exist, same as every other ownership failure.
        if (visible.isEmpty() && treated.stream().noneMatch(k -> k.startsWith(patientId + ":"))) {
            throw new ResourceNotFoundException("Patient not found");
        }
        return visible;
    }

    @Transactional
    public void delete(Integer patientId, Integer reportId) {
        MedicalReport report = reportRepository.findByIdAndPatientId(reportId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        reportRepository.delete(report);
        storage.delete(report.getReportDataUrl());
    }

    // ---------------------------------------------------------------- internals

    /**
     * Every (account, subject) pair this doctor has an appointment with.
     *
     * <p>A composite string rather than a pair of ids because the dependent half
     * is nullable, and a null in a Set key is the kind of thing that quietly
     * matches everything.
     */
    private Set<String> treatedSubjects(String doctorId) {
        Set<String> keys = new HashSet<>();
        for (Appointment a : appointmentRepository.findAllForDoctor(doctorId)) {
            keys.add(subjectKey(a.getPatient().getId(),
                    a.getFamilyMember() == null ? null : a.getFamilyMember().getId()));
        }
        return keys;
    }

    private static String subjectKey(Integer patientId, Integer familyMemberId) {
        return patientId + ":" + Objects.toString(familyMemberId, "self");
    }

    private DownloadedFile readFile(MedicalReport report) {
        return new DownloadedFile(
                storage.read(report.getReportDataUrl()),
                report.getReportName(),
                report.getContentType() == null
                        ? "application/octet-stream" : report.getContentType());
    }

    private RecordResponse toDto(MedicalReport r) {
        FamilyMember subject = r.getFamilyMember();
        return new RecordResponse(
                r.getId(),
                r.getReportName(),
                r.getReportType(),
                r.getUploadDate() == null ? null : r.getUploadDate().format(DATE),
                FileStorageService.humanSize(r.getFileSizeBytes() == null ? 0 : r.getFileSizeBytes()),
                r.getUploadedByType() == null ? "PATIENT" : r.getUploadedByType().name(),
                "/records/" + r.getId() + "/download",
                subject == null ? null : subject.getId(),
                subject == null ? r.getPatient().getFullName() : subject.getFullName(),
                subject == null ? null : subject.getRelation().name());
    }

    public record DownloadedFile(byte[] bytes, String filename, String contentType) {
    }
}
