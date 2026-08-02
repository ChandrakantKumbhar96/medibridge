package com.medibridge.appointment.entity;

import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.patient.entity.FamilyMember;
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

    /**
     * The account holder - who booked, who paid, and who may cancel. Always set,
     * even when the visit is for a dependent.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Who the visit is actually for. Null means the account holder themselves.
     *
     * <p>Kept separate from {@link #patient} on purpose: the payer, the canceller
     * and the person in the chair are not always the same, and collapsing them
     * would put a child's name on a refund and the parent's age on a
     * prescription. The database enforces that this dependent belongs to
     * {@code patient} via a composite foreign key - see V12.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "family_member_id")
    private FamilyMember familyMember;

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

    /**
     * The completed consultation this free revisit descends from; null on an
     * ordinary booking.
     *
     * <p>UNIQUE at the DB level - see V13. That index, not any field or flag, is
     * what limits a visit to one free follow-up: two concurrent requests off the
     * same parent lose there rather than at a check that raced.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_appointment_id", unique = true)
    private Appointment parentAppointment;

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

    /**
     * Stamped by the join endpoint, which is the only route to the room URL.
     * Attendance is therefore observed, not self-reported - neither side can
     * claim to have turned up without having asked for the link.
     */
    @Column(name = "patient_joined_at")
    private LocalDateTime patientJoinedAt;

    @Column(name = "doctor_joined_at")
    private LocalDateTime doctorJoinedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "no_show_by")
    private NoShowParty noShowBy;

    @Column(name = "no_show_marked_at")
    private LocalDateTime noShowMarkedAt;

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

    /** True when this visit is for a dependent rather than the account holder. */
    public boolean isForDependent() {
        return familyMember != null;
    }

    /** True when this booking is the free revisit off an earlier consultation. */
    public boolean isFollowUp() {
        return parentAppointment != null;
    }

    /**
     * Last instant a free revisit off this consultation may be booked, or null
     * if it never earned one.
     *
     * <p>Runs from {@code completed_at} rather than the slot time, so a doctor
     * writing their notes up late does not quietly shorten the patient's window.
     */
    public LocalDateTime followUpEligibleUntil(int windowDays) {
        return status == AppointmentStatus.COMPLETED && completedAt != null
                ? completedAt.plusDays(windowDays)
                : null;
    }

    /**
     * The name that belongs on the doctor's list, the prescription and the
     * consultation notes - the person being treated, not the person who paid.
     */
    public String subjectName() {
        return familyMember != null ? familyMember.getFullName() : patient.getFullName();
    }

    /**
     * Date of birth of the person being treated.
     *
     * <p>This is the field that makes the feature clinical rather than cosmetic:
     * a paediatric dose is computed from the child's age, and reading the
     * parent's here would print the wrong one on a real prescription.
     */
    public java.time.LocalDate subjectDateOfBirth() {
        return familyMember != null ? familyMember.getDateOfBirth() : patient.getDateOfBirth();
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

    /**
     * True once a doctor-initiated move has left the patient at a time they
     * never chose. Their cancellation cutoff is waived while this holds - see
     * {@code AppointmentService.cancelAsPatient}.
     */
    public boolean wasMovedByDoctor() {
        return rescheduledBy == ActorRole.DOCTOR;
    }

    /**
     * Who failed to attend, decided only from observed join timestamps.
     *
     * <p>Returns null when the consultation clearly happened (both joined) or
     * when it is too early to judge - the caller must not treat null as
     * "nobody was at fault".
     */
    public NoShowParty decideNoShowParty() {
        boolean patientCame = patientJoinedAt != null;
        boolean doctorCame = doctorJoinedAt != null;

        if (patientCame && doctorCame) return null;
        if (!patientCame && !doctorCame) return NoShowParty.BOTH;
        return patientCame ? NoShowParty.DOCTOR : NoShowParty.PATIENT;
    }

    public enum ActorRole {
        PATIENT, DOCTOR, ADMIN, SYSTEM
    }

    public enum NoShowParty {
        PATIENT, DOCTOR, BOTH
    }
}
