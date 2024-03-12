package com.fox2code.foxevents;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Should be used by method handler to define an event handler for a specific event
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
    /**
     * Default priority used in handlers
     * @since 1.0.0
     */
    int DEFAULT_PRIORITY = 1000;
    /**
     * @return Should receive the event, even if it's cancelled.
     * @since 1.0.0
     */
    boolean ignoreCancelled() default false;

    /**
     * @return The event priority, highest is executed first.
     * @since 1.0.0
     */
    int priority() default DEFAULT_PRIORITY;
}
