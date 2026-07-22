package com.medibridge.review.entity;

import com.medibridge.appointment.entity.Appointment;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "rating")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    private Integer id;

    /** Unique - one review per appointment. */
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    /**
     * The column is TINYINT (1-5 needs no more), but Java's Short defaults to
     * SMALLINT. The explicit JDBC type keeps entity and schema in agreement.
     */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private Short stars;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_experience", nullable = false)
    private OverallExperience overallExperience;

    /**
     * "What stood out?" is multi-select in RateExperience.jsx, so this is a
     * collection table rather than a single column - one ENUM column would have
     * silently kept only the first tag.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rating_highlight",
            joinColumns = @JoinColumn(name = "rating_id"))
    @Column(name = "highlight", nullable = false)
    @Builder.Default
    private Set<Highlight> highlights = new LinkedHashSet<>();

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    /**
     * MySQL fills this via DEFAULT CURRENT_TIMESTAMP. @Generated makes Hibernate
     * read the value back after the INSERT - without it the field stays null in
     * the response returned immediately after creation.
     */
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum OverallExperience {
        Excellent, Good, Okay, Poor
    }

    /**
     * Values match the MySQL ENUM literals exactly (spaces included), so
     * EnumType.STRING round-trips without a converter.
     */
    public enum Highlight {
        BEDSIDE_MANNER("Bedside Manner"),
        CLEAR_EXPLANATIONS("Clear Explanations"),
        FOLLOW_UP_CARE("Follow-up Care"),
        ACCURATE_DIAGNOSIS("Accurate Diagnosis"),
        FRIENDLY_STAFF("Friendly Staff");

        private final String label;

        Highlight(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static Highlight fromLabel(String label) {
            for (Highlight h : values()) {
                if (h.label.equalsIgnoreCase(label) || h.name().equalsIgnoreCase(label)) {
                    return h;
                }
            }
            throw new IllegalArgumentException("Unknown highlight: " + label);
        }
    }
}
