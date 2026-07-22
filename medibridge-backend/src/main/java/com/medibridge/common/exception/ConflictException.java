package com.medibridge.common.exception;

/** Duplicate email, slot already booked, review already submitted. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
