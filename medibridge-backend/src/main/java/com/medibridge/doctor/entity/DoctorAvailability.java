package com.medibridge.doctor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;

/**
 * The recurring weekly pattern the doctor edits on the Manage Schedule screen
 * ({@code { day, available, morning, afternoon }}). Concrete bookable slots are
 * generated from this into {@link DoctorSchedule}.
 */
@Entity
@Table(name = "doctor_availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "availability_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @Column(nullable = false)
    private Boolean morning;

    @Column(nullable = false)
    private Boolean afternoon;

    @PrePersist
    void applyDefaults() {
        if (isAvailable == null) isAvailable = true;
        if (morning == null) morning = false;
        if (afternoon == null) afternoon = false;
    }
}
