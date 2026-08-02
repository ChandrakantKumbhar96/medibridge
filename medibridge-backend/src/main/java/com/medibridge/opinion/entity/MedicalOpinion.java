package com.medibridge.opinion.entity;

import com.medibridge.appointment.entity.Appointment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A reviewing specialist's written judgement on another clinician's diagnosis.
 *
 * <p>Kept separate from {@code Prescription} on purpose. A prescription exists to
 * carry a list of medicines; a second opinion frequently concludes that nothing
 * should change, and expressing that through an empty medicine table would make
 * a complete answer look like an unfinished record.
 */
@Entity
@Table(name = "medical_opinion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalOpinion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "opinion_id")
    private Integer id;

    /**
     * One appointment, one opinion - enforced by a UNIQUE constraint on the
     * column, not by an application check.
     */
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    /**
     * The diagnosis being reviewed, as the patient presented it.
     *
     * <p>Stored even though it originates elsewhere: without it the document
     * cannot be read back later to see what was actually under review, which is
     * the whole point of the record.
     */
    @Column(name = "original_diagnosis", columnDefinition = "TEXT", nullable = false)
    private String originalDiagnosis;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String findings;

    /** The verdict, as a column so it can be listed and filtered, not just read. */
    @Column(name = "agrees_with_original", nullable = false)
    private Boolean agreesWithOriginal;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String recommendation;

    @Column(name = "suggested_tests", columnDefinition = "TEXT")
    private String suggestedTests;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void applyDefaults() {
        if (issuedAt == null) issuedAt = LocalDateTime.now();
    }
}
