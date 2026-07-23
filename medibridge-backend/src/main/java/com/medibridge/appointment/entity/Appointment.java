package com.medibridge.appointment.entity;

import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    /**
     * The booked slot. Unique at the DB level - that constraint, not any
     * application check, is what makes double booking impossible.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", unique = true)
    private DoctorSchedule schedule;

    @Column(name = "request_date", insertable = false, updatable = false)
    private LocalDateTime requestDate;

    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;

    @Column(nullable = false)
    private AppointmentStatus status;

    @Column(name = "consult_type", length = 50)
    private String consultType;

    /**
     * Price agreed when the appointment was created, not looked up at payment
     * time. A doctor raising their rate afterwards must not change what this
     * patient owes, and the invoice must stay reproducible.
     */
    @Column(name = "booked_fee", precision = 10, scale = 2)
    private BigDecimal bookedFee;

    @Column(name = "platform_fee", precision = 10, scale = 2)
    private BigDecimal platformFee;

    /** bookedFee + platformFee - the exact amount charged. */
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * A PENDING_PAYMENT appointment holds its slot only until this instant.
     * Without it, closing the checkout tab would block that slot forever.
     */
    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(columnDefinition = "TEXT")
    private String reason;

    /** Generated on acceptance and emailed to the patient. */
    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "meeting_sent_at")
    private LocalDateTime meetingSentAt;

    // The link only works inside this window - a forwarded URL should not let a
    // stranger join a consultation weeks later.
    @Column(name = "meeting_join_from")
    private LocalDateTime meetingJoinFrom;

    @Column(name = "meeting_valid_until")
    private LocalDateTime meetingValidUntil;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by")
    private ActorRole cancelledBy;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    /**
     * Capped by policy so a slot cannot be shuffled indefinitely.
     *
     * <p>TINYINT declared explicitly: Java's Short defaults to SMALLINT, which
     * ddl-auto:validate rejects against the column type. Same trap as
     * rating.stars.
     */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "reschedule_count", nullable = false)
    private Short rescheduleCount;

    /** The first date booked, preserved so the history is not lost on a move. */
    @Column(name = "original_date")
    private LocalDateTime originalDate;

    @Column(name = "last_rescheduled_at")
    private LocalDateTime lastRescheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "rescheduled_by")
    private ActorRole rescheduledBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void applyDefaults() {
        if (status == null) status = AppointmentStatus.PENDING_PAYMENT;
        if (consultType == null) consultType = "Consultation";
        if (rescheduleCount == null) rescheduleCount = 0;
    }

    /** True once the consultation time has actually arrived. */
    public boolean hasStarted() {
        return appointmentDate != null && !appointmentDate.isAfter(LocalDateTime.now());
    }

    /** The join link is live only inside the configured window. */
    public boolean isMeetingJoinable() {
        if (meetingLink == null || meetingJoinFrom == null || meetingValidUntil == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(meetingJoinFrom) && !now.isAfter(meetingValidUntil);
    }

    public boolean isHoldExpired() {
        return status == AppointmentStatus.PENDING_PAYMENT
                && holdExpiresAt != null
                && holdExpiresAt.isBefore(LocalDateTime.now());
    }

    public enum ActorRole {
        PATIENT, DOCTOR, ADMIN, SYSTEM
    }
}
