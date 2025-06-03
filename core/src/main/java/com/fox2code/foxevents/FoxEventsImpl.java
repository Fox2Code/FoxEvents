package com.fox2code.foxevents;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * Default implementation of {@link FoxEvents}
 * @since 1.3.0
 */
public class FoxEventsImpl extends FoxEvents {
    static final FoxEventsImpl DEFAULT_IMPL = new FoxEventsImpl();

    public FoxEventsImpl() {}

    @Override
    public void registerEvents(@NotNull Object handler) throws EventRegistrationException {
        for (EventCallback eventCallback : this.getEventCallbacks(handler)) {
            this.registerEventCallback(eventCallback);
        }
    }

    @Override
    public void registerEvents(@NotNull Object handler,@Nullable BooleanSupplier validator)
            throws EventRegistrationException {
        for (EventCallback eventCallback : this.getEventCallbacks(handler, validator)) {
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
