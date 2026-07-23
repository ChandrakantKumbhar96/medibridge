package com.medibridge.payout.entity;

import com.medibridge.appointment.entity.Appointment;
import com.medibridge.doctor.entity.Doctor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * What the platform owes a doctor for one completed consultation.
 *
 * <p>A ledger row, not a running balance. A single "balance" column on the
 * doctor would turn every correction into a destructive update with no history;
 * here each consultation's economics are permanent, and any balance is a SUM.
 *
 * <p>{@code commissionRate} is snapshotted for the same reason the appointment
 * snapshots its fee: changing the platform's rate must not rewrite what past
 * consultations earned.
 */
@Entity
@Table(name = "doctor_earning")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "earning_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    /** Unique - one earning per appointment, the guard against double payment. */
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    /**
     * The consultation fee only. The platform fee the patient paid on top is
     * platform revenue and never enters the doctor's ledger.
     */
    @Column(name = "gross_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id")
    private DoctorPayout payout;

    @Column(name = "reversal_reason", length = 255)
    private String reversalReason;

    @Column(name = "earned_at", insertable = false, updatable = false)
    private LocalDateTime earnedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @PrePersist
    void applyDefaults() {
        if (status == null) status = Status.PENDING;
    }

    /**
     * Splits a consultation fee into commission and net payable.
     *
     * <p>Rounds the commission and derives net by subtraction rather than
     * rounding both: rounding each independently can leave the two parts not
     * summing back to the gross, which shows up as a rupee going missing in
     * reconciliation.
     */
    public static DoctorEarning of(Appointment appointment, BigDecimal commissionPercent) {
        BigDecimal gross = appointment.getBookedFee() == null
                ? BigDecimal.ZERO : appointment.getBookedFee();

        BigDecimal commission = gross
                .multiply(commissionPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return DoctorEarning.builder()
                .doctor(appointment.getDoctor())
                .appointment(appointment)
                .grossAmount(gross)
                .commissionRate(commissionPercent)
                .commissionAmount(commission)
                .netAmount(gross.subtract(commission))
                .status(Status.PENDING)
                .build();
    }

    public enum Status {
        /** Earned, not yet included in a payout. */
        PENDING,
        /** Included in a payout batch. */
        SETTLED,
        /** Consultation was refunded - the doctor is no longer owed for it. */
        REVERSED
    }
}
