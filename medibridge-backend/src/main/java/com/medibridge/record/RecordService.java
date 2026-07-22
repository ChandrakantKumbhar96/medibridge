package com.medibridge.record;

import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.patient.PatientRepository;
import com.medibridge.patient.entity.Patient;
import com.medibridge.record.dto.RecordResponse;
import com.medibridge.record.entity.MedicalReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MedicalReportRepository reportRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final FileStorageService storage;

    @Transactional(readOnly = true)
    public List<RecordResponse> listForPatient(Integer patientId) {
        return reportRepository.findByPatientIdOrderByUploadDateDesc(patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public RecordResponse upload(Integer patientId, MultipartFile file,
                                 String reportName, String reportType) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (reportName == null || reportName.isBlank()) {
            throw new BadRequestException("Report name is required");
        }

        String storedName = storage.store(file);

        MedicalReport report = MedicalReport.builder()
                .patient(patient)
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
     * Patient downloading their own report.
     *
     * <p>Queried by (id, patientId) rather than id alone, so changing the id in
     * the URL yields 404 rather than someone else's file. 404 not 403 - a 403
     * would confirm the report exists.
     */
    @Transactional(readOnly = true)
    public DownloadedFile downloadAsPatient(Integer patientId, Integer reportId) {
        MedicalReport report = reportRepository.findByIdAndPatientId(reportId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return readFile(report);
    }

    /**
     * A doctor may read a report only for a patient they have actually treated.
     * Holding ROLE_DOCTOR is not by itself sufficient.
     */
    @Transactional(readOnly = true)
    public DownloadedFile downloadAsDoctor(String doctorId, Integer reportId) {
        MedicalReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        boolean hasTreated = appointmentRepository.findAllForDoctor(doctorId).stream()
                .anyMatch(a -> a.getPatient().getId().equals(report.getPatient().getId()));

        if (!hasTreated) {
            throw new ResourceNotFoundException("Report not found");
        }
        return readFile(report);
    }

    @Transactional(readOnly = true)
    public List<RecordResponse> listForDoctorsPatient(String doctorId, Integer patientId) {
        boolean hasTreated = appointmentRepository.findAllForDoctor(doctorId).stream()
                .anyMatch(a -> a.getPatient().getId().equals(patientId));

        if (!hasTreated) {
            throw new ResourceNotFoundException("Patient not found");
        }
        return listForPatient(patientId);
    }

    @Transactional
    public void delete(Integer patientId, Integer reportId) {
        MedicalReport report = reportRepository.findByIdAndPatientId(reportId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        reportRepository.delete(report);
        storage.delete(report.getReportDataUrl());
    }

    private DownloadedFile readFile(MedicalReport report) {
        return new DownloadedFile(
                storage.read(report.getReportDataUrl()),
                report.getReportName(),
                report.getContentType() == null
                        ? "application/octet-stream" : report.getContentType());
    }

    private RecordResponse toDto(MedicalReport r) {
        return new RecordResponse(
                r.getId(),
                r.getReportName(),
                r.getReportType(),
                r.getUploadDate() == null ? null : r.getUploadDate().format(DATE),
                FileStorageService.humanSize(r.getFileSizeBytes() == null ? 0 : r.getFileSizeBytes()),
                r.getUploadedByType() == null ? "PATIENT" : r.getUploadedByType().name(),
                "/records/" + r.getId() + "/download");
    }

    public record DownloadedFile(byte[] bytes, String filename, String contentType) {
    }
}
