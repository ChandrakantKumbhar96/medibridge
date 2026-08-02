package com.medibridge.common.exception;

/**
 * Phone one-time-code rejected.
 *
 * <p>Separate from BadCredentialsException for the same reason as
 * {@link OAuthException}: the handler answers that one with a fixed
 * "Invalid email or password" so password logins cannot be used to enumerate
 * registered emails, and it would overwrite anything thrown from the OTP path
 * with wording about a password the caller never typed.
 *
 * <p>The anti-enumeration rule still applies here, it is just enforced at the
 * throw site instead: a wrong code, an expired one, one already consumed, one
 * burnt by too many guesses, and a number that was never sent anything all
 * raise this with the same message. Told apart, each is a hint about what to
 * change next.
 */
public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String message) {
        super(message);
    }
}
