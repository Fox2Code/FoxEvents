package com.fox2code.foxevents.unsafe;

import com.fox2code.foxevents.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;

/**
 * Unsafe version of FoxEvents, for speed!
 */
public abstract class UnsafeFoxEvents extends FoxEvents {
    private static final MethodHandles.Lookup
            TRUSTED_LOOKUP = UnsafeFoxEventsJVMHelper.getTrustedLookup();
    private static final UnsafeFoxEventsJVMHelper.JVMImplementation
            JVM_IMPLEMENTATION = UnsafeFoxEventsJVMHelper.getJVMImplementation();

    public UnsafeFoxEvents() {}

    /**
     * Used to get the EventCallbacks of an event handler.
     * @param instance the instance to get the event callbacks of
     * @return an {@link ArrayList} of all registered event callbacks
     * @throws EventRegistrationException if we failed to acquire all event callbacks
     * @since 1.3.0
     */
    @Contract(pure = true)
    protected final ArrayList<EventCallback> getEventCallbacksUnsafe(
            @NotNull Object instance) throws EventRegistrationException {
        return this.getEventCallbacksUnsafe(instance, null, false, null);
    }

    /**
     * Used to get the EventCallbacks of an event handler.
     * @param instance the instance to get the event callbacks of
     * @param validator the validator, or null if the result is always {@code true}.
     * @return an {@link ArrayList} of all registered event callbacks
     * @throws EventRegistrationException if we failed to acquire all event callbacks
     * @since 1.3.0
     */
    @Contract(pure = true)
    protected final @NotNull ArrayList<EventCallback> getEventCallbacksUnsafe(
            @NotNull Object instance,@Nullable BooleanSupplier validator) throws EventRegistrationException {
        return this.getEventCallbacksUnsafe(instance, validator, false, null);
    }

    /**
     * Used to get the EventCallbacks of an event handler.
     * @param instance the instance to get the event callbacks of
     * @param validator the validator, or null if the result is always {@code true}.
     * @param ignoreInvalid should it skip invalid event callback instead of throwing {@link EventRegistrationException}.
     * @return an {@link ArrayList} of all registered event callbacks
     * @throws EventRegistrationException if we failed to acquire all event callbacks
     * @since 1.3.0
     */
    @Contract(pure = true)
    protected final @NotNull ArrayList<EventCallback> getEventCallbacksUnsafe(
            @NotNull Object instance,@Nullable BooleanSupplier validator,
            boolean ignoreInvalid) throws EventRegistrationException {
        return this.getEventCallbacksUnsafe(instance, validator, ignoreInvalid, null);
    }

    /**
     * Used to get the EventCallbacks of an event handler, unsafe/speedy version.
     * @param instance the instance to get the event callbacks of
     * @param validator the validator, or null if the result is always {@code true}.
     * @param ignoreInvalid should it skip invalid event callback instead of throwing {@link EventRegistrationException}.
     * @param lookup the lookup used to resolve methods
     * @return an {@link ArrayList} of all registered event callbacks
     * @throws EventRegistrationException if we failed to acquire all event callbacks
     * @since 1.3.0
     */
    @Contract(pure = true)
    protected final @NotNull ArrayList<EventCallback> getEventCallbacksUnsafe(
            @NotNull Object instance, @Nullable BooleanSupplier validator,
            boolean ignoreInvalid, @Nullable MethodHandles.Lookup lookup) throws EventRegistrationException {
        if (JVM_IMPLEMENTATION == null) {
            return this.getEventCallbacks(instance, validator, ignoreInvalid,
                    lookup == null ? TRUSTED_LOOKUP : lookup);
        }
        this.ensureInstanceAccess();
        ArrayList<EventCallback> eventCallbacks = new ArrayList<>();
        boolean isStatic = instance instanceof Class;
        Class<?> handlerClass = isStatic ? (Class<?>) instance : instance.getClass();
        for (Method method : JVM_IMPLEMENTATION.getMethods(handlerClass)) {
            if (method.isBridge() || method.isSynthetic() ||
                    Modifier.isStatic(method.getModifiers()) != isStatic) continue;
            EventHandler eventHandler = method.getAnnotation(EventHandler.class);
            if (eventHandler == null) continue;
            Class<?>[] args = method.getParameterTypes();
            if (args.length != 1) continue;
            EventHolder<?> eventHolder = EventHolder.getHolderFromEventRaw(args[0]);
            MethodHandle methodHandle;
            try {
                methodHandle = JVM_IMPLEMENTATION.unReflectAndBind(method, isStatic ? null : instance);
            } catch (IllegalAccessException e) {
                if (ignoreInvalid) continue;
                throw new EventRegistrationException("Failed to un-reflect method " + method.getName(), e);
            }
            eventCallbacks.add(this.makeEventCallbackRaw(eventHolder, isStatic ? null : instance,
                    methodHandle, eventHandler.ignoreCancelled(), eventHandler.priority(), validator));
        }
        return eventCallbacks;
    }

    /**
     * @return if unsafe acceleration is in use.
     */
    protected final boolean isUsingUnsafeAcceleration() {
        return JVM_IMPLEMENTATION != null;
    }
}
