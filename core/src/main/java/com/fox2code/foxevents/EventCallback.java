package com.fox2code.foxevents;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Represent a method with {@link EventHandler} registered via a {@link #holder}
 */
public final class EventCallback {
    public final EventHolder<?> eventHolder;
    public final Object holder;
    public final MethodHandle eventCallback;
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

    @Contract(pure = true)
    public @NotNull EventCallback withValidator(@Nullable BooleanSupplier validator) {
        return new EventCallback(this.eventHolder, holder,
                this.eventCallback, this.ignoreCancelled, this.priority, validator);
    }

    public boolean isInvalid() {
        return this.validator != null && !this.validator.getAsBoolean();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        EventCallback that = (EventCallback) object;

        if (ignoreCancelled != that.ignoreCancelled) return false;
        if (priority != that.priority) return false;
        if (!eventHolder.equals(that.eventHolder)) return false;
        if (!Objects.equals(holder, that.holder)) return false;
        return eventCallback.equals(that.eventCallback);
    }

    @Override
    public int hashCode() {
        int result = eventHolder.hashCode();
        result = 31 * result + (holder != null ? holder.hashCode() : 0);
        result = 31 * result + eventCallback.hashCode();
        result = 31 * result + (ignoreCancelled ? 1 : 0);
        result = 31 * result + priority;
        return result;
    }

    void callForEvent(@NotNull Event event) {
        try {
            if (this.holder != null) {
                this.eventCallback.invoke(this.holder, event);
            } else {
                this.eventCallback.invoke(event);
            }
        } catch (Throwable t) {
            FoxEvents.getFoxEventsSoft().onEventError(event, this, t);
        }
    }
}
