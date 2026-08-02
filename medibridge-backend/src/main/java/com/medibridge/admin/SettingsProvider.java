package com.medibridge.admin;

import com.medibridge.admin.entity.SystemSetting;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Typed access to the {@code system_settings} table.
 *
 * <p>Pricing and policy live in the database rather than in constants so an
 * admin can change the platform fee or the cancellation window without a
 * redeploy - which is how a real product works.
 *
 * <p>Every getter takes a fallback: a missing or corrupted row must not stop a
 * patient from booking.
 */
@Component
@RequiredArgsConstructor
public class SettingsProvider {

    private static final Logger log = LoggerFactory.getLogger(SettingsProvider.class);

    public static final String PLATFORM_FEE = "platform_fee";
    public static final String SLOT_HOLD_MINUTES = "slot_hold_minutes";
    public static final String FREE_CANCELLATION_HOURS = "free_cancellation_hours";
    public static final String PARTIAL_REFUND_PERCENT = "partial_refund_percent";
    public static final String MEETING_JOIN_BEFORE_MIN = "meeting_join_before_min";
    public static final String MEETING_VALID_AFTER_MIN = "meeting_valid_after_min";
    public static final String REMINDER_HOURS_BEFORE = "reminder_hours_before";
    public static final String MAX_APPOINTMENTS_PER_DAY = "max_appointments_per_day";
    public static final String QUEUE_MAX_DELAY_MIN = "queue_max_delay_minutes";
    public static final String QUEUE_MIN_DELAY_MIN = "queue_min_delay_minutes";
    public static final String SECOND_OPINION_FEE_PERCENT = "second_opinion_fee_percent";
    public static final String SECOND_OPINION_MIN_REPORTS = "second_opinion_min_reports";
    public static final String FOLLOW_UP_ENABLED = "follow_up_enabled";
    public static final String FOLLOW_UP_WINDOW_DAYS = "follow_up_window_days";
    public static final String OTP_LENGTH = "otp_length";
    public static final String OTP_TTL_MINUTES = "otp_ttl_minutes";
    public static final String OTP_MAX_ATTEMPTS = "otp_max_attempts";
    public static final String OTP_RESEND_COOLDOWN_SECONDS = "otp_resend_cooldown_seconds";
    public static final String NEXT_AVAILABLE_FLOOR_MIN = "next_available_floor_minutes";
    public static final String NEXT_AVAILABLE_WINDOW_HOURS = "next_available_window_hours";
    public static final String MAX_RESCHEDULES = "max_reschedules";
    public static final String RESCHEDULE_MIN_HOURS = "reschedule_min_hours";
    public static final String NO_SHOW_GRACE_MINUTES = "no_show_grace_minutes";

    private final SystemSettingRepository repository;

    /**
     * Deployment defaults for the free follow-up, from application.yml.
     *
     * <p>Every other fallback in this class is a literal, which is fine for a
     * value nobody tunes per environment. These two are: a staging build wants
     * the window wide enough to demo in one sitting, and the kill switch has to
     * be flippable without a migration. They are still only the FALLBACK - a
     * system_settings row overrides them, same as everywhere else.
     */
    @Value("${medibridge.follow-up.enabled:true}")
    private boolean followUpEnabledDefault;

    @Value("${medibridge.follow-up.window-days:7}")
    private int followUpWindowDaysDefault;

    /**
     * Phone login defaults, same arrangement: yml is the deployment default,
     * a system_settings row overrides it.
     *
     * <p>These four are the entire security argument for a six-digit secret -
     * how long it lives, how many guesses it survives, and how often one can be
     * requested - so they are configuration rather than constants, and none of
     * them is hardcoded anywhere else.
     */
    @Value("${medibridge.otp.length:6}")
    private int otpLengthDefault;

    @Value("${medibridge.otp.ttl-minutes:5}")
    private int otpTtlMinutesDefault;

    @Value("${medibridge.otp.max-attempts:5}")
    private int otpMaxAttemptsDefault;

    @Value("${medibridge.otp.resend-cooldown-seconds:60}")
    private int otpResendCooldownSecondsDefault;

    @Transactional(readOnly = true)
    public int getInt(String key, int fallback) {
        try {
            return repository.findById(key)
                    .map(SystemSetting::getValue)
                    .map(Integer::parseInt)
                    .orElse(fallback);
        } catch (Exception e) {
            log.warn("Setting '{}' unreadable, using fallback {}: {}", key, fallback, e.getMessage());
            return fallback;
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getMoney(String key, BigDecimal fallback) {
        try {
            return repository.findById(key)
                    .map(SystemSetting::getValue)
                    .map(BigDecimal::new)
                    .orElse(fallback);
        } catch (Exception e) {
            log.warn("Setting '{}' unreadable, using fallback {}: {}", key, fallback, e.getMessage());
            return fallback;
        }
    }

    public BigDecimal platformFee() {
        return getMoney(PLATFORM_FEE, BigDecimal.ZERO);
    }

    public int slotHoldMinutes() {
        return getInt(SLOT_HOLD_MINUTES, 15);
    }

    public int freeCancellationHours() {
        return getInt(FREE_CANCELLATION_HOURS, 24);
    }

    public int partialRefundPercent() {
        return getInt(PARTIAL_REFUND_PERCENT, 50);
    }

    public int meetingJoinBeforeMinutes() {
        return getInt(MEETING_JOIN_BEFORE_MIN, 15);
    }

    public int meetingValidAfterMinutes() {
        return getInt(MEETING_VALID_AFTER_MIN, 60);
    }

    public int reminderHoursBefore() {
        return getInt(REMINDER_HOURS_BEFORE, 24);
    }

    /**
     * Ceiling on the "running late" estimate. A room left open, or a no-show the
     * sweep has not reached, would otherwise report a delay in hours - a number
     * that tells a waiting patient nothing and reads as a bug.
     */
    public int queueMaxDelayMinutes() {
        return getInt(QUEUE_MAX_DELAY_MIN, 120);
    }

    /** Below this the doctor is simply on time - no delay is shown. */
    public int queueMinDelayMinutes() {
        return getInt(QUEUE_MIN_DELAY_MIN, 5);
    }

    /**
     * A second opinion as a percentage of the doctor's normal fee - 150 meaning
     * 150%. Reviewing another clinician's case file is more work than a first
     * consultation, and pricing it as a percentage keeps that proportional to
     * each doctor's own rate rather than a flat surcharge.
     */
    public int secondOpinionFeePercent() {
        return getInt(SECOND_OPINION_FEE_PERCENT, 150);
    }

    /** Documents a patient must have on file to book a second opinion; 0 disables. */
    public int secondOpinionMinReports() {
        return getInt(SECOND_OPINION_MIN_REPORTS, 1);
    }

    /**
     * Whether a completed consultation earns one free revisit.
     *
     * <p>Stored as 0/1 rather than "true"/"false" so it reads through the same
     * {@link #getInt} path as every other row; the column is a VARCHAR and a
     * second parser would be a second thing to get wrong.
     */
    public boolean followUpEnabled() {
        return getInt(FOLLOW_UP_ENABLED, followUpEnabledDefault ? 1 : 0) != 0;
    }

    /**
     * Days after {@code completed_at} in which the free revisit may be booked.
     *
     * <p>Measured from completion, not from the appointment date: a
     * consultation written up two days late would otherwise silently eat two
     * days of the patient's window.
     */
    public int followUpWindowDays() {
        return getInt(FOLLOW_UP_WINDOW_DAYS, followUpWindowDaysDefault);
    }

    /** Digits in a login code. Six is what every Indian OTP looks like. */
    public int otpLength() {
        return getInt(OTP_LENGTH, otpLengthDefault);
    }

    /**
     * How long a code stays usable. Short enough that a code read off a lock
     * screen an hour later is worthless, long enough to survive a slow carrier.
     */
    public int otpTtlMinutes() {
        return getInt(OTP_TTL_MINUTES, otpTtlMinutesDefault);
    }

    /**
     * Guesses a code survives before the row is burnt.
     *
     * <p>This is the number that makes six digits safe: 5 tries against a
     * million values, once, is not an attack.
     */
    public int otpMaxAttempts() {
        return getInt(OTP_MAX_ATTEMPTS, otpMaxAttemptsDefault);
    }

    /** Seconds before the same number may ask for another code. Each one costs money. */
    public int otpResendCooldownSeconds() {
        return getInt(OTP_RESEND_COOLDOWN_SECONDS, otpResendCooldownSecondsDefault);
    }

    /** Minimum lead time a "next available" offer must give the patient to pay. */
    public int nextAvailableFloorMinutes() {
        return getInt(NEXT_AVAILABLE_FLOOR_MIN, 15);
    }

    /** How far ahead "next available" searches for an open slot. */
    public int nextAvailableWindowHours() {
        return getInt(NEXT_AVAILABLE_WINDOW_HOURS, 24);
    }

    /** Times a patient may move an appointment before they must cancel and rebook. */
    public int maxReschedules() {
        return getInt(MAX_RESCHEDULES, 2);
    }

    /** Minimum notice a patient reschedule must give; a doctor reschedule is unrestricted. */
    public int rescheduleMinHours() {
        return getInt(RESCHEDULE_MIN_HOURS, 4);
    }

    /** Minutes past the room closing before an unattended appointment counts as a no-show. */
    public int noShowGraceMinutes() {
        return getInt(NO_SHOW_GRACE_MINUTES, 15);
    }
}
