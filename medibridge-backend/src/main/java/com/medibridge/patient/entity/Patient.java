package com.medibridge.patient.entity;

import com.medibridge.common.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fields mirror PatientRegisterForm.jsx one-for-one.
 *
 * <p>Sequential int PK is fine here: unlike doctors there is no public patient
 * listing to enumerate, and record access is gated by an ownership check in the
 * service layer rather than by id opacity.
 */
@Entity
@Table(name = "patient")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Integer id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** Null for Google accounts - they never set a password here. */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    /**
     * Google's stable subject id. Accounts are matched on this rather than on
     * email, because a Google account's email address can change while the sub
     * never does.
     */
    @Column(name = "google_sub", length = 64, unique = true)
    private String googleSub;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 20)
    private String phone;

    @Column(name = "another_number", length = 20)
    private String anotherNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    // Nullable since V3: a Google user has none of these until they complete
    // their profile on the settings screen.
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('Male','Female','Other')")
    private Gender gender;

    @Column(name = "blood_group", length = 5)
    private String bloodGroup;

    @Column(name = "reason_of_consult", columnDefinition = "TEXT")
    private String reasonOfConsult;

    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void applyDefaults() {
        if (status == null) {
            status = AccountStatus.ACTIVE;
        }
        if (authProvider == null) {
            authProvider = AuthProvider.LOCAL;
        }
    }

    /** True once the Google user has filled in the fields registration would have required. */
    public boolean isProfileComplete() {
        return dateOfBirth != null && gender != null
                && bloodGroup != null && phone != null;
    }

    public enum Gender {
        Male, Female, Other
    }

    public enum AuthProvider {
        LOCAL, GOOGLE
    }
}
