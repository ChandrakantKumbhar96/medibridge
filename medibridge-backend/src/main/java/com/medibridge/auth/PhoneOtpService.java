package com.medibridge.auth;

import com.medibridge.admin.SettingsProvider;
import com.medibridge.auth.entity.PhoneOtp;
import com.medibridge.common.exception.TooManyRequestsException;
import com.medibridge.notification.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * The life of a login code: issue it, text it, and decide exactly once whether
 * a submitted code was it.
 *
 * <p>Deliberately knows nothing about patients. {@link AuthService} owns the
 * question of which account a verified number belongs to (and whether to create
 * one); this class owns the question of whether the number was verified at all.
 */
@Service
@RequiredArgsConstructor
public class PhoneOtpService {

    private final PhoneOtpRepository repository;
    private final SettingsProvider settings;
    private final SmsService smsService;

    private final SecureRandom random = new SecureRandom();

    /**
     * Issues a code for {@code e164} and texts it over whatever channel
     * {@code medibridge.sms.channel} names.
     *
     * <p>The send happens inside this transaction and is allowed to throw. That
     * is the point: if the gateway refuses, the row rolls back with it, so the
     * cooldown does not start and the patient can immediately try again rather
     * than being told to wait a minute for a code that was never sent.
     *
     * @throws TooManyRequestsException the cooldown since the last code has not elapsed
     */
    @Transactional
    public void issue(String e164) {
        int cooldownSeconds = settings.otpResendCooldownSeconds();

        repository.findFirstByPhoneE164OrderByIdDesc(e164).ifPresent(latest -> {
            long elapsed = Duration.between(latest.getCreatedAt(), LocalDateTime.now())
                    .toSeconds();
            if (elapsed < cooldownSeconds) {
                long remaining = cooldownSeconds - elapsed;
                throw new TooManyRequestsException(
                        "A code was just sent. Please wait " + remaining
                                + " seconds before requesting another.", remaining);
            }
        });

        String code = randomCode(settings.otpLength());
        int ttlMinutes = settings.otpTtlMinutes();

        repository.save(PhoneOtp.builder()
                .phoneE164(e164)
                .codeHash(hash(e164, code))
                .expiresAt(LocalDateTime.now().plusMinutes(ttlMinutes))
                .attempts(0)
                .build());

        smsService.sendNow(e164, "Your MediBridge verification code is " + code
                + ". It expires in " + ttlMinutes
                + " minutes. MediBridge staff will never ask you for it.");
    }

    /**
     * Spends the code, or reports that it was not spendable.
     *
     * <p>Every rejection returns the same {@code false}: wrong code, expired
     * code, one already used, and a number with no code at all are one answer,
     * because telling them apart tells an attacker which of those four to fix.
     *
     * <p><b>Runs in its own transaction on purpose.</b> A failed attempt has to
     * be counted, and the caller's response to {@code false} is to throw - which
     * would roll the increment back and leave the attempt cap permanently at
     * zero. Committing here is what makes the counter real.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean consume(String e164, String code) {
        PhoneOtp otp = repository.lockLatestForPhone(e164).orElse(null);
        if (otp == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        // Already spent, timed out, or burnt by too many guesses. Checked before
        // the counter moves, so a burnt row stays burnt - the correct code does
        // not get in behind an exhausted one.
        if (otp.getConsumedAt() != null
                || otp.getExpiresAt().isBefore(now)
                || otp.getAttempts() >= settings.otpMaxAttempts()) {
            return false;
        }

        otp.setAttempts(otp.getAttempts() + 1);

        if (!MessageDigest.isEqual(
                hash(e164, code).getBytes(StandardCharsets.UTF_8),
                otp.getCodeHash().getBytes(StandardCharsets.UTF_8))) {
            repository.save(otp);
            return false;
        }

        otp.setConsumedAt(now);
        repository.save(otp);
        return true;
    }

    /**
     * Uniform across the range - {@code nextInt(bound)} rather than a modulo of
     * a wider draw, which would make the low codes fractionally likelier.
     */
    private String randomCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * Bound to the number so the same code issued to two people does not produce
     * the same hash - the codes are short enough that a shared value would
     * otherwise be visible in the table.
     */
    private static String hash(String e164, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest((e164 + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
