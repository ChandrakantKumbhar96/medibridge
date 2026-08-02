package com.medibridge.record.entity;

import com.medibridge.patient.entity.FamilyMember;
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

    /** The owning account. Every access check resolves through this. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Whose document this is. Null means the account holder's own.
     *
     * <p>Unlike a prescription, a report is uploaded outside any appointment, so
     * it has no visit to inherit its subject from and needs its own column. The
     * composite foreign key in V12 keeps this dependent tied to {@code patient}.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "family_member_id")
    private FamilyMember familyMember;

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
