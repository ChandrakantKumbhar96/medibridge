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
    AUTO_EXPIRED("AutoExpired", "cancelled"),

    /**
     * Nobody attended. Kept distinct from CANCELLED all the way to the UI:
     * the money rule is the opposite - a cancellation refunds, a no-show
     * usually does not - and a patient shown "cancelled" beside "no refund"
     * concludes they were charged a cancellation fee. Badge.jsx has its own
     * `no_show` entry.
     */
    NO_SHOW("NoShow", "no_show");

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
