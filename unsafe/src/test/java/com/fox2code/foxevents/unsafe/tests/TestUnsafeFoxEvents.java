package com.fox2code.foxevents.unsafe.tests;

import com.fox2code.foxevents.EventCallback;
import com.fox2code.foxevents.FoxEvents;
import com.fox2code.foxevents.unsafe.UnsafeFoxEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.util.function.BooleanSupplier;

public class TestUnsafeFoxEvents extends UnsafeFoxEvents {
    public static TestUnsafeFoxEvents INSTANCE = new TestUnsafeFoxEvents();

    static {
        FoxEvents.setFoxEvents(INSTANCE);
    }

    private TestUnsafeFoxEvents() {}

    @Override
    public void registerEvents(@NotNull Object handler) {
        for (EventCallback eventCallback : this.getEventCallbacksUnsafe(handler)) {
            this.registerEventCallback(eventCallback);
        }
    }

    @Override
    public void registerEvents(@NotNull Object handler,@Nullable BooleanSupplier validator) {
        for (EventCallback eventCallback : this.getEventCallbacksUnsafe(handler, validator)) {
            this.registerEventCallback(eventCallback);
        }
    }

    public int registerEventsIgnoreInvalid(@NotNull Object handler) {
        int handlerCount = 0;
        for (EventCallback eventCallback : this.getEventCallbacksUnsafe(handler, null, true)) {
            this.registerEventCallback(eventCallback);
            handlerCount++;
        }
        return handlerCount;
    }

    public void registerEventsWithLookup(@NotNull Object handler, @Nullable MethodHandles.Lookup lookup) {
        for (EventCallback eventCallback : this.getEventCallbacksUnsafe(handler, null, false, lookup)) {
            this.registerEventCallback(eventCallback);
        }
    }

    public boolean isUsingUnsafeAccelerationTesting() {
        return this.isUsingUnsafeAcceleration();
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
