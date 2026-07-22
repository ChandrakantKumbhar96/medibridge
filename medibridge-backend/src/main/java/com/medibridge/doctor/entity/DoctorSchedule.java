package com.medibridge.doctor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A concrete bookable slot on a specific date.
 *
 * <p>{@code isBooked} is a read cache for fast slot listings - it is NOT the
 * booking guard. Two concurrent requests can both read {@code isBooked=false};
 * what actually prevents the double booking is the UNIQUE constraint on
 * {@code appointment.schedule_id}.
 */
@Entity
@Table(name = "doctor_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_booked", nullable = false)
    private Boolean isBooked;

    @PrePersist
    void applyDefaults() {
        if (isBooked == null) isBooked = false;
    }
}
