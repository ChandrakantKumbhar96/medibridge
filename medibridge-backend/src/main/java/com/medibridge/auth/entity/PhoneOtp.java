package com.medibridge.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One issued login code.
 *
 * <p>Every rule that makes a six-digit secret safe is a column here rather than
 * state in the service: when it stops being valid, how many guesses it has
 * absorbed, and whether it has already been spent. A restart, a second
 * instance, or a deploy mid-login all forget in-memory counters; the row
 * remembers.
 *
 * <p>Keyed on the number, not on a patient - the account may not exist yet.
 */
@Entity
@Table(name = "phone_otp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Long id;

    /** Normalised by PhoneNumbers.toE164 - the same string the code was texted to. */
    @Column(name = "phone_e164", nullable = false, length = 16)
    private String phoneE164;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts;

    /** Set on the one successful verification; a second one finds it non-null. */
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
