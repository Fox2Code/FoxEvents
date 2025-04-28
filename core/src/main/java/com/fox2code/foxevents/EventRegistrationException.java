package com.fox2code.foxevents;

/**
 * Thrown when a method registration failed.
 * @since 1.2.0
 */
public final class EventRegistrationException extends RuntimeException {
    /**
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public EventRegistrationException(String message) {
        super(message);
    }

    /**
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public EventRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
