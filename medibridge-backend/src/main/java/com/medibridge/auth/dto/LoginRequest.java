package com.medibridge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Matches the body LoginPage.jsx / AdminLoginPage.jsx dispatch.
 *
 * <p>{@code role} selects which identity table to authenticate against. It is
 * NOT a grant of authority: sending {@code role: "admin"} with a patient's
 * credentials simply looks in the admin table, finds nothing, and fails.
 * Authority always comes from the row actually loaded.
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password,

        @NotBlank(message = "Role is required")
        String role
) {
}
