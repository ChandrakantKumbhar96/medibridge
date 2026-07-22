package com.medibridge.common.enums;

/**
 * The three identity tables. The frontend sends this as {@code role} on login
 * ("patient" | "doctor" | "admin") and it tells us which table to authenticate
 * against - but it is never trusted as a grant of authority. Authority always
 * comes from the row we actually loaded from the database.
 */
public enum UserType {
    PATIENT,
    DOCTOR,
    ADMIN;

    public static UserType fromRequest(String role) {
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        return switch (role.trim().toLowerCase()) {
            case "patient" -> PATIENT;
            case "doctor" -> DOCTOR;
            case "admin" -> ADMIN;
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
    }

    /** Spring Security expects the ROLE_ prefix on authorities. */
    public String authority() {
        return "ROLE_" + name();
    }

    /** Lowercase form the React app stores in localStorage as user.role. */
    public String toFrontend() {
        return name().toLowerCase();
    }
}
