package com.avanzada.exception;

/**
 * Thrown when a request lifecycle transition is not allowed (e.g. wrong current state).
 * Controllers may map this to 409 Conflict.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
