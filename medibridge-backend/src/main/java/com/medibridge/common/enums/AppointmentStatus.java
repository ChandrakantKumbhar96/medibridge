package com.medibridge.common.enums;

/**
 * Appointment lifecycle.
 *
 * <p>The DB values are richer than what the React Badge component understands,
 * so {@link #toFrontend()} maps them onto the strings the badge colour map in
 * components/common/Badge.jsx actually has entries for. Without this mapping
 * every status renders in the default blue.
 */
public enum AppointmentStatus {
    PENDING_PAYMENT("PendingPayment", "pending"),
    REQUESTED("Requested", "pending"),
    ACCEPTED("Accepted", "confirmed"),
    REJECTED("Rejected", "cancelled"),
    RESCHEDULED("Rescheduled", "pending"),
    COMPLETED("Completed", "confirmed"),
    CANCELLED("Cancelled", "cancelled"),
    AUTO_EXPIRED("AutoExpired", "cancelled");

    private final String dbValue;
    private final String frontendValue;

    AppointmentStatus(String dbValue, String frontendValue) {
        this.dbValue = dbValue;
        this.frontendValue = frontendValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    /** Status string the React Badge component can colour. */
    public String toFrontend() {
        return frontendValue;
    }

    public boolean isActive() {
        return this == REQUESTED || this == ACCEPTED || this == RESCHEDULED;
    }

    public boolean isCancellable() {
        return this == PENDING_PAYMENT || this == REQUESTED || this == ACCEPTED
                || this == RESCHEDULED;
    }
}
