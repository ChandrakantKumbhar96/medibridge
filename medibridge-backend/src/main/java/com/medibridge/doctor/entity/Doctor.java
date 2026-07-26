package com.medibridge.doctor.entity;

import com.medibridge.common.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Doctor identity + profile.
 *
 * <p>The PK is a UUID string rather than a sequential int so that public doctor
 * listings cannot be enumerated or scraped by walking ids. Note this is
 * anti-enumeration only - it is not an access control mechanism. Authorization
 * is enforced separately in the service layer.
 */
@Entity
@Table(name = "doctor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @Column(name = "doctor_id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "specialization_id", nullable = false)
    private Specialization specialization;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "consultation_duration_min")
    private Integer consultationDurationMin;

    @Column(columnDefinition = "TEXT")
    private String bio;

    /** e.g. "MBBS, MD (Medicine), DM (Cardiology)". */
    @Column(length = 255)
    private String qualifications;

    /** Comma-separated languages the doctor consults in, e.g. "English, Hindi". */
    @Column(length = 150)
    private String languages;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg;

    @Column(name = "rating_count")
    private Integer ratingCount;

    // AccountStatusConverter (autoApply) handles the UPPER_CASE <-> lowercase mapping
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /**
     * Hibernate will not generate the id for a manually assigned String PK, so
     * we assign it here. Done in @PrePersist rather than the builder so it is
     * impossible to construct a Doctor without one.
     */
    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = AccountStatus.PENDING;
        }
        if (ratingAvg == null) {
            ratingAvg = BigDecimal.ZERO;
        }
        if (ratingCount == null) {
            ratingCount = 0;
        }
    }
}
