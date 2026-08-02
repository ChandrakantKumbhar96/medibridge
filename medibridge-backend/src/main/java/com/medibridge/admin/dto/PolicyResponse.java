package com.medibridge.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The subset of {@link com.medibridge.admin.SettingsProvider} a patient (or
 * the chat assistant answering on their behalf) needs to reason about
 * cancellation, reschedule, no-show and follow-up rules.
 *
 * <p>Public and read-only: every value here is already implicit in what a
 * patient sees on the cancel/reschedule dialogs, so there is nothing to
 * protect by hiding it. See {@code SecurityConfig}'s permitAll list and
 * {@code publicPaths.js}'s mirror of it.
 */
public record PolicyResponse(

        @JsonProperty("free_cancellation_hours")
        int freeCancellationHours,

        @JsonProperty("partial_refund_percent")
        int partialRefundPercent,

        @JsonProperty("max_reschedules")
        int maxReschedules,

        @JsonProperty("reschedule_min_hours")
        int rescheduleMinHours,

        @JsonProperty("no_show_grace_minutes")
        int noShowGraceMinutes,

        @JsonProperty("follow_up_enabled")
        boolean followUpEnabled,

        @JsonProperty("follow_up_window_days")
        int followUpWindowDays,

        @JsonProperty("second_opinion_fee_percent")
        int secondOpinionFeePercent,

        @JsonProperty("second_opinion_min_reports")
        int secondOpinionMinReports
) {
}
