package com.fox2code.foxevents.tests;

import com.fox2code.foxevents.EventCallback;
import com.fox2code.foxevents.FoxEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.util.function.BooleanSupplier;

public class TextFoxEvents extends FoxEvents {
    public static TextFoxEvents INSTANCE = new TextFoxEvents();

    static {
        FoxEvents.setFoxEvents(INSTANCE);
    }

    private TextFoxEvents() {}

    @Override
    public void registerEvents(@NotNull Object handler) {
        for (EventCallback eventCallback : this.getEventCallbacks(handler)) {
            this.registerEventCallback(eventCallback);
        }
    }

    @Override
    public void registerEvents(@NotNull Object handler,@Nullable BooleanSupplier validator) {
        for (EventCallback eventCallback : this.getEventCallbacks(handler, validator)) {
            this.registerEventCallback(eventCallback);
        }
    }

    public int registerEventsIgnoreInvalid(@NotNull Object handler) {
        int handlerCount = 0;
        for (EventCallback eventCallback : this.getEventCallbacks(handler, null, true)) {
            this.registerEventCallback(eventCallback);
            handlerCount++;
        }
        return handlerCount;
    }

    public void registerEventsWithLookup(@NotNull Object handler, @Nullable MethodHandles.Lookup lookup) {
        for (EventCallback eventCallback : this.getEventCallbacks(handler, null, false, lookup)) {
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
