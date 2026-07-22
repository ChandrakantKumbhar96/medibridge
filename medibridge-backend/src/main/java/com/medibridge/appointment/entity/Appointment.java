package com.medibridge.appointment.entity;

import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(columnDefinition = "TEXT")
    private String reason;

    /** Generated on acceptance and emailed to the patient. */
    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "meeting_sent_at")
    private LocalDateTime meetingSentAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void applyDefaults() {
        if (status == null) status = AppointmentStatus.PENDING_PAYMENT;
        if (consultType == null) consultType = "Consultation";
    }
}
