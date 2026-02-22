package com.avanzada.exception;

/**
 * Thrown when a request by id is not found.
 * Controllers may map this to 404 Not Found.
 */
public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(String message) {
        super(message);
    }
}
