package com.medibridge.prescription;

import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.patient.PatientRepository;
import com.medibridge.prescription.dto.PrescriptionRequest;
import com.medibridge.prescription.dto.PrescriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Doctor-issued prescriptions and the medical history PDF")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PatientRepository patientRepository;

    @PostMapping("/prescriptions")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Issue a prescription")
    public PrescriptionResponse create(@CurrentUser SecurityUser me,
                                       @Valid @RequestBody PrescriptionRequest request) {
        return prescriptionService.create(me.getId(), request);
    }

    @GetMapping("/prescriptions")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List my prescriptions")
    public List<PrescriptionResponse> myPrescriptions(@CurrentUser SecurityUser me) {
        return prescriptionService.listForPatient(me.idAsInt());
    }

    @GetMapping("/prescriptions/{prescriptionId}/pdf")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @Operation(summary = "Download a prescription PDF", description = "Patient gets their own, doctor gets ones they issued.")
    public ResponseEntity<byte[]> prescriptionPdf(@CurrentUser SecurityUser me,
                                                  @PathVariable Integer prescriptionId) {
        byte[] pdf = switch (me.getUserType()) {
            case PATIENT -> prescriptionService.prescriptionPdfForPatient(
                    me.idAsInt(), prescriptionId);
            case DOCTOR -> prescriptionService.prescriptionPdfForDoctor(
                    me.getId(), prescriptionId);
            default -> throw new IllegalStateException("Unsupported role");
        };

        return pdfResponse(pdf, "prescription-" + prescriptionId + ".pdf");
    }

    @GetMapping("/records/medical-history/pdf")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Download my full medical history PDF")
    public ResponseEntity<byte[]> medicalHistoryPdf(@CurrentUser SecurityUser me) {
        var patient = patientRepository.findById(me.idAsInt())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        return pdfResponse(prescriptionService.medicalHistoryPdf(patient),
                "medical-history-" + patient.getId() + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
