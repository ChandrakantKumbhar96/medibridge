package com.medibridge.common.exception;

/**
 * Federated sign-in failure.
 *
 * <p>Separate from BadCredentialsException so the real reason can be shown.
 * Password logins are deliberately answered with a single vague message to
 * avoid revealing which emails are registered; that concern does not apply to
 * "your Google token expired" or "Google Sign-In is not configured", and hiding
 * those just leaves the user stuck.
 */
public class OAuthException extends RuntimeException {
    public OAuthException(String message) {
        super(message);
    }
}
