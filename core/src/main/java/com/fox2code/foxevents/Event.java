package com.fox2code.foxevents;

import org.jetbrains.annotations.Contract;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event class that can be extended
 * @since 1.0.0
 */
public abstract class Event {
    private EventHolder<?> initializingHolder;
    /**
     * Tell if event is cancelled, this is only for use by the instantiated event itself
     * @see #isCancelled()
     * @see #setCancelled(boolean)
     * @since 1.0.0
     */
    protected boolean cancelled;

    public Event() {}

    /**
     * A hook to allow to provide an event holder in a more efficient way, if the returned
     * result is {@code null} or does not match the event, it will be safely ignored.
     * @return the event holder matching this event
     * @since 1.0.0
     */
    protected EventHolder<?> provideEventHolder() {
        return null;
    }

    /**
     * @return if event is cancelled
     * @since 1.0.0
     */
    @Contract(pure = true)
    public final boolean isCancelled() {
        return this.cancelled;
    }


    /**
     * @param cancelled if the event is cancelled
     * @since 1.0.0
     */
    public final void setCancelled(boolean cancelled) {
        if (!(this instanceof Cancellable)) {
            throw new RuntimeException("Event is not cancellable");
        }
        this.cancelled = cancelled;
    }

    /**
     * Fast way to dispatch an event, run faster if called repeatedly on the same instance
     * @since 1.0.0
     */
    public final void callEvent() {
        EventHolder<?> eventHolder = this.initializingHolder;
        if (eventHolder != null) {
            eventHolder.callEventRaw(this);
            return;
        }
        eventHolder = this.provideEventHolder();
        if (eventHolder != null && eventHolder.peekEvent() == this.getClass()) {
            this.initializingHolder = eventHolder;
        } else {
            eventHolder = this.initializingHolder =
                    EventHolder.getHolderFromEventRaw(this.getClass());
        }
        eventHolder.callEventRaw(this);
    }

    /**
     * Called when an event has finished to be processed, used to reset event status if needed
     * @since 1.0.0
     */
    protected void cleanUp() {
        this.cancelled = false;
    }

    /**
     * Mark the event as cancellable
     * @since 1.0.0
     */
    public interface Cancellable {
        @Contract(pure = true)
        boolean isCancelled();

        void setCancelled(boolean cancelled);
    }


    /**
     * When used, tell FoxEvents to also use parent listeners for the event.
     * @since 1.0.0
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DelegateEvent {}
}
