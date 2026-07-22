package com.medibridge.auth.entity;

import com.medibridge.common.enums.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Only the SHA-256 hash of the token is stored. If the database leaks, the
 * hashes cannot be replayed as bearer tokens.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Integer id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean revoked;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void applyDefaults() {
        if (revoked == null) {
            revoked = false;
        }
    }

    public boolean isUsable() {
        return !Boolean.TRUE.equals(revoked) && expiresAt.isAfter(LocalDateTime.now());
    }
}
