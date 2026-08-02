package com.medibridge.common.util;

/**
 * One definition of "the same number", shared by everything that needs one.
 *
 * <p>This started as a private helper inside {@code SmsService}, where it
 * decided who a message was addressed to. Phone login needs the identical
 * reduction to decide which account a caller is claiming - and if the two ever
 * drifted apart, the system would text a number it could not then resolve back
 * to an account. They are the same function because they have to be.
 *
 * <p>The E.164 form is also what {@code patient.phone_e164} stores and what its
 * UNIQUE index is built on, so the bounds here are the column's bounds.
 */
public final class PhoneNumbers {

    /** Assumed when the caller gives no country code. */
    private static final String DEFAULT_COUNTRY_CODE = "+91";

    /** E.164 allows 15 digits; the leading '+' makes 16, which is the column width. */
    private static final int MAX_LENGTH = 16;

    /** Below this it is a typo, an extension or a landline stub - not reachable. */
    private static final int MIN_LENGTH = 8;

    private PhoneNumbers() {
    }

    /**
     * Reduces "+91 90000 11111", "9000011111" and "09000011111" to
     * "+919000011111".
     *
     * @return the E.164 form, or null when the input cannot be one - callers
     *         treat null as "no number", never as a key
     */
    public static String toE164(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.isBlank()) {
            return null;
        }
        if (!digits.startsWith("+")) {
            // A local number written with a trunk prefix: 09000011111.
            digits = DEFAULT_COUNTRY_CODE + digits.replaceFirst("^0+", "");
        }
        return digits.length() < MIN_LENGTH || digits.length() > MAX_LENGTH ? null : digits;
    }
}
