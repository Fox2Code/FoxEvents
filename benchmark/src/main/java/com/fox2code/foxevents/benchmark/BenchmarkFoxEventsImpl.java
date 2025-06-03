package com.fox2code.foxevents.benchmark;

import com.fox2code.foxevents.EventCallback;
import com.fox2code.foxevents.EventRegistrationException;
import com.fox2code.foxevents.FoxEvents;
import com.fox2code.foxevents.FoxEventsImpl;
import com.fox2code.foxevents.unsafe.UnsafeFoxEvents;
import com.fox2code.foxevents.unsafe.UnsafeFoxEventsImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * Variant of {@link FoxEventsImpl} being able to act like {@link UnsafeFoxEventsImpl}
 * @since 1.3.0
 */
public class BenchmarkFoxEventsImpl extends UnsafeFoxEvents {
    static final BenchmarkFoxEventsImpl INSTANCE = new BenchmarkFoxEventsImpl();

    public BenchmarkFoxEventsImpl() {}

    private boolean unsafeMode;

    @Override
    public void registerEvents(@NotNull Object handler) throws EventRegistrationException {
        for (EventCallback eventCallback : this.unsafeMode ?
                this.getEventCallbacksUnsafe(handler) :
                this.getEventCallbacks(handler)) {
            this.registerEventCallback(eventCallback);
        }
    }

    @Override
    public void registerEvents(@NotNull Object handler,@Nullable BooleanSupplier validator)
            throws EventRegistrationException {
        for (EventCallback eventCallback : this.unsafeMode ?
                this.getEventCallbacksUnsafe(handler, validator) :
                this.getEventCallbacks(handler, validator)) {
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

    public void setUnsafeMode(boolean unsafeMode) {
        this.unsafeMode = unsafeMode;
    }
}
