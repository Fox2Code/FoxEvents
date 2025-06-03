package com.fox2code.foxevents.unsafe;

import com.fox2code.foxevents.EventCallback;
import com.fox2code.foxevents.EventRegistrationException;
import com.fox2code.foxevents.FoxEvents;
import com.fox2code.foxevents.FoxEventsImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * Variant of {@link FoxEventsImpl} using {@link UnsafeFoxEvents}
 * @since 1.3.0
 */
public class UnsafeFoxEventsImpl extends UnsafeFoxEvents {
    public UnsafeFoxEventsImpl() {}

    @Override
    public void registerEvents(@NotNull Object handler) throws EventRegistrationException {
        for (EventCallback eventCallback : this.getEventCallbacksUnsafe(handler)) {
            this.registerEventCallback(eventCallback);
        }
    }

    @Override
    public void registerEvents(@NotNull Object handler,@Nullable BooleanSupplier validator)
            throws EventRegistrationException {
        for (EventCallback eventCallback : this.getEventCallbacksUnsafe(handler, validator)) {
            this.registerEventCallback(eventCallback);
        }
    }

    @Override
    public void unregisterEvents(@NotNull Object handler) {
        if (handler.getClass().getClassLoader() != FoxEvents.class.getClassLoader()) {
            // Cross class loader unregister is a bit iffy, but implementation can always be overridden.
            throw new IllegalArgumentException("Handler doesn't use same class loader as FoxEvents");
        }
        this.unregisterEventsForClassLoader(FoxEvents.class.getClassLoader(), handler);
    }
}
