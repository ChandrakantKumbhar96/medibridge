package com.medibridge.payout.entity;

import com.medibridge.doctor.entity.Doctor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A settlement batch: everything one doctor earned in one period, paid together.
 *
 * <p>Batched rather than paid per consultation because bank transfers cost money
 * and reconciliation is per-transfer. Real marketplaces settle on a cycle for
 * exactly this reason.
 */
@Entity
@Table(name = "doctor_payout")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payout_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private Integer consultations;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal commission;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /** Bank/UPI reference once the transfer is actually made. */
    @Column(name = "payout_ref", length = 64)
    private String payoutRef;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    void applyDefaults() {
        if (status == null) status = Status.PENDING;
        if (consultations == null) consultations = 0;
        if (grossAmount == null) grossAmount = BigDecimal.ZERO;
        if (commission == null) commission = BigDecimal.ZERO;
        if (netAmount == null) netAmount = BigDecimal.ZERO;
    }

    public enum Status {
        /** Batch created, transfer not yet made. */
        PENDING,
        PAID,
        FAILED
    }
}
