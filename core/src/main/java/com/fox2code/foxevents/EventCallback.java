package com.fox2code.foxevents;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Represent a method with {@link EventHandler} registered via a {@link #holder}
 */
public final class EventCallback {
    public final EventHolder<?> eventHolder;
    public final Object holder;
    final MethodHandle eventCallback;
    public final boolean ignoreCancelled;
    public final int priority;
    public final BooleanSupplier validator;

    EventCallback(EventHolder<?> eventHolder, Object holder, MethodHandle eventCallback,
                         boolean ignoreCancelled, int priority, BooleanSupplier validator) {
        this.eventHolder = eventHolder;
        this.holder = holder;
        this.eventCallback = eventCallback;
        this.ignoreCancelled = ignoreCancelled;
        this.priority = priority;
        this.validator = validator;
    }

    /**
     * @param validator the validator to this event callback
     * @return the new event callback using the new validator
     * @since 1.0.0
     */
    @Contract(pure = true)
    public @NotNull EventCallback withValidator(@Nullable BooleanSupplier validator) {
        return new EventCallback(this.eventHolder, holder,
                this.eventCallback, this.ignoreCancelled, this.priority, validator);
    }

    /**
     * @return if the current EventCallback is invalid according to the validator
     * @since 1.0.0
     */
    public boolean isInvalid() {
        return this.validator != null && !this.validator.getAsBoolean();
    }

    /**
     * @return if the current EventCallback is registered
     * @since 1.2.0
     */
    public boolean isRegistered() {
        return this.eventHolder.isEventCallbackRegistered(this);
    }

    /**
     * Call an event for this event callback only
     * @param event to dispatch
     * @since 1.2.0
     */
    public void callForEvent(@NotNull Event event) {
        Objects.requireNonNull(event, "event == null");
        this.eventHolder.getEvent().cast(event);
        this.callForEventRaw(event);
    }

    void callForEventRaw(@NotNull Event event) {
        try {
            this.eventCallback.invoke(event);
        } catch (Throwable t) {
            FoxEvents.getFoxEventsSoft().onEventError(event, this, t);
        }
    }
}
