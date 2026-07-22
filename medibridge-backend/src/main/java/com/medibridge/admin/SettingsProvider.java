package com.medibridge.admin;

import com.medibridge.admin.entity.SystemSetting;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final SystemSettingRepository repository;

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
}
