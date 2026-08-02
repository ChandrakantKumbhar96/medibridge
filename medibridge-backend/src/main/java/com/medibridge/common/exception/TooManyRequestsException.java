package com.medibridge.common.exception;

import lombok.Getter;

/**
 * A rate limit the application itself enforces, as opposed to the per-IP one in
 * {@code AuthRateLimitFilter}.
 *
 * <p>The two answer different questions. The filter caps how fast one caller may
 * hammer an endpoint and forgets everything on restart; this one carries limits
 * that must hold across instances and restarts because breaking them costs
 * money or safety - the OTP resend cooldown being the case it was added for.
 *
 * <p>Carries its own retry hint so the caller can show a countdown rather than
 * guess.
 */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
