package com.medibridge.record.entity;

import com.medibridge.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "report_name", nullable = false, length = 150)
    private String reportName;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    /** Path on disk, never the raw bytes - BLOBs in MySQL are a bad trade here. */
    @Column(name = "report_data_url", nullable = false, length = 255)
    private String reportDataUrl;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "uploaded_by_type")
    private UploaderType uploadedByType;

    @Column(name = "uploaded_by_id", length = 36)
    private String uploadedById;

    @Column(name = "upload_date", insertable = false, updatable = false)
    private LocalDateTime uploadDate;

    @PrePersist
    void applyDefaults() {
        if (uploadedByType == null) uploadedByType = UploaderType.PATIENT;
        if (fileSizeBytes == null) fileSizeBytes = 0L;
    }

    public enum UploaderType {
        PATIENT, DOCTOR, SYSTEM
    }
}
