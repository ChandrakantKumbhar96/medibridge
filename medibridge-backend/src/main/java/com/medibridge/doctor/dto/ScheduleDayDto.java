package com.medibridge.doctor.dto;

/**
 * Matches the weekly rows on Manage Schedule:
 * {@code { day: 'Monday', available: true, morning: true, afternoon: false }}.
 */
public record ScheduleDayDto(
        String day,
        Boolean available,
        Boolean morning,
        Boolean afternoon
) {
}
