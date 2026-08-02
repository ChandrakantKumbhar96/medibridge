package com.medibridge.common.enums;

/**
 * The values {@code appointment.consult_type} is expected to hold.
 *
 * <p>Constants rather than a Java enum because the column is a free
 * {@code VARCHAR(50)} and the frontend has always been able to send its own
 * label - an enum would reject data the schema accepts, and turn a cosmetic
 * mismatch into a failed booking.
 *
 * <p>Lives in {@code common} rather than in either module that cares. The
 * appointment module needs it to price and gate a second opinion; the opinion
 * module needs it to refuse an appointment that is not one. Putting it in either
 * would make the two import each other for a string.
 */
public final class ConsultType {

    public static final String CONSULTATION = "Consultation";
    public static final String FOLLOW_UP = "Follow-up";
    public static final String SECOND_OPINION = "Second Opinion";

    private ConsultType() {
    }

    /** Case-insensitive: the label arrives from the client, not from an enum. */
    public static boolean isSecondOpinion(String consultType) {
        return SECOND_OPINION.equalsIgnoreCase(consultType);
    }
}
