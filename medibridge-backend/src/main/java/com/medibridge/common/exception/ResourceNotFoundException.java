package com.medibridge.common.exception;

/**
 * Also thrown deliberately on ownership failures (patient A requesting patient
 * B's record). Returning 404 rather than 403 avoids confirming that the
 * resource exists at all.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
