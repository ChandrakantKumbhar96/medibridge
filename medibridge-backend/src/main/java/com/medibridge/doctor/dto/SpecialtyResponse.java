package com.medibridge.doctor.dto;

/**
 * Backs the specialty cards on the booking wizard's first step, which read
 * {@code { name, emoji, doctors }}.
 */
public record SpecialtyResponse(
        Integer id,
        String name,
        String emoji,
        String description,
        long doctors
) {
}
