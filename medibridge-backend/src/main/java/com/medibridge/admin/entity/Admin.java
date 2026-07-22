package com.medibridge.admin.entity;

import com.medibridge.common.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * There is no admin self-registration screen - rows are seeded (see DataSeeder)
 * or created by another admin. AdminLoginPage.jsx authenticates by email only,
 * which is why this table has no username column.
 */
@Entity
@Table(name = "admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Integer id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 50)
    private String title;

    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void applyDefaults() {
        if (status == null) {
            status = AccountStatus.ACTIVE;
        }
        if (title == null) {
            title = "System Administrator";
        }
    }
}
