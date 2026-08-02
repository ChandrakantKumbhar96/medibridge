package com.medibridge.patient.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A dependent the account holder books and holds records for - a child, a
 * spouse, an ageing parent.
 *
 * <p>Deliberately not a {@link Patient}. A dependent has no email, no password
 * and no session; modelling them as a patient row would mean making the login
 * columns nullable and weakening {@code UNIQUE(email)}, which is the one column
 * authentication cannot afford to have holes in.
 *
 * <p>Every dependent is owned by exactly one patient, and that ownership is what
 * every authorization check resolves through: nothing is ever fetched by
 * {@code familyMemberId} alone.
 */
@Entity
@Table(name = "family_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "family_member_id")
    private Integer id;

    /** The account holder. Never null - a dependent cannot exist unowned. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient owner;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * Required, unlike on {@link Patient}. Age is the whole reason this record
     * exists clinically - a paediatric dose is computed from it - so a dependent
     * with an unknown date of birth is not a usable profile.
     */
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    /**
     * Constants are named to match the MySQL ENUM literals exactly, so no
     * AttributeConverter is needed - the same trick {@link Patient.Gender} uses.
     * A converter only becomes unavoidable when the literal is not a legal Java
     * identifier (see {@code HighlightConverter} and 'Bedside Manner').
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('Male','Female','Other')")
    private Patient.Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "ENUM('Child','Spouse','Parent','Sibling','Other')")
    private Relation relation;

    @Column(name = "blood_group", length = 5)
    private String bloodGroup;

    @Column(length = 20)
    private String phone;

    /** Set instead of deleting - see the migration for why. */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }

    public enum Relation {
        Child, Spouse, Parent, Sibling, Other
    }
}
