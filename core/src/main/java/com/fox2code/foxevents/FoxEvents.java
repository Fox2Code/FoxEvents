package com.fox2code.foxevents;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

/**
 * FoxEvent implementation, extend this class and call {@link #setFoxEvents(FoxEvents)} to customize default behaviour.
 *
 * @since 1.0.0
 */
public abstract class FoxEvents {
    public static final Logger LOGGER = Logger.getLogger("FoxEvents");
    final Comparator<EventCallback> comparator = this::compare;
    static FoxEvents foxEvents;
    boolean unsafeAccess;

    /**
     * Default constructor for FoxEvents implementation
     * @since 1.0.0
     */
    public FoxEvents() {}

    /**
     * Used to set the FoxEvents backing implementation.
     *
     * @param foxEvents instance
     * @since 1.0.0
     */
    public static void setFoxEvents(@NotNull FoxEvents foxEvents) {
        Objects.requireNonNull(foxEvents, "FoxEvents implementation cannot be null.");
        if (FoxEvents.foxEvents != null) {
            throw new RuntimeException("Can't change default FoxEvent implementations");
        }
        FoxEvents.foxEvents = foxEvents;
    }

    /**
     * Get and set the active FoxEvents helper, call to {@link #setFoxEvents(FoxEvents)}
     * won't work after it has been called.
     *
     * @return the active FoxEvents
     * @since 1.0.0
     */
    public static @NotNull FoxEvents getFoxEvents() {
        FoxEvents foxEvents = FoxEvents.foxEvents;
        if (foxEvents == null) {
            foxEvents = FoxEventsImpl.DEFAULT_IMPL;
            FoxEvents.foxEvents = foxEvents;
        }
        return foxEvents;
    }

    /**
     * Get the active FoxEvents helper, without triggering an instance initialization,
     * the returned FoxEvents instance may be unable to register Events,
     * if you need to register events handlers call {@link #getFoxEvents()} instead.
     *
     * @return the active FoxEvents
     * @since 1.0.0
     */
    @Contract(pure = true)
    public static @NotNull FoxEvents getFoxEventsSoft() {
        FoxEvents foxEvents = FoxEvents.foxEvents;
        return foxEvents != null ? foxEvents :
                FoxEventsImpl.DEFAULT_IMPL;
    }

    protected final void ensureInstanceAccess() {
        if (this != FoxEvents.foxEvents && !this.unsafeAccess) {
            throw new SecurityException("Current object isn't the FoxEvents instance");
        }
    }

    /**
     * Used to force callbacks to revaluate validators, this will be done lazily.
     * <p>
     * Validators can be reevaluated early with {@link EventHolder#ensureBaked()}
     *
     * @since 1.0.0
     */
    protected final void invalidateCallbackValidators() {
        this.ensureInstanceAccess();
        EventHolder.validationModCount++;
    }

    /**
     * Make an event callback, with a stable ABI across versions
     * @param eventHolder the event holder
     * @param holder the instance holder
     * @param eventCallback the event callback
     * @param ignoreCancelled if the handler should be called even if the event is cancelled
     * @param priority the priority of the event
     * @param validator the validator of the event
     * @return the new {@link EventCallback}
     * @since 1.0.0
     */
    @Contract(pure = true)
    protected final @NotNull EventCallback makeEventCallback(
            @NotNull EventHolder<?> eventHolder,@Nullable Object holder,@NotNull MethodHandle eventCallback,
            boolean ignoreCancelled, int priority,@Nullable BooleanSupplier validator) {
        this.ensureInstanceAccess();
        Objects.requireNonNull(eventHolder, "eventHolder == null");
        Objects.requireNonNull(eventCallback, "eventCallback == null");
        if (holder != null) {
            // Ensure ABI compat with 1.1.0
            eventCallback = eventCallback.bindTo(holder);
        }
        return new EventCallback(eventHolder, holder, eventCallback,
                ignoreCancelled, priority, validator);
    }

    /**
     * Make an event callback without ensuring version compatibility.
     * @param eventHolder the event holder
     * @param holder the instance holder
     * @param eventCallback the event callback
     * @param ignoreCancelled if the handler should be called even if the event is cancelled
     * @param priority the priority of the event
     * @param validator the validator of the event
     * @return the new {@link EventCallback}
     * @since 1.3.0
     */
    @ApiStatus.Internal
    @Contract(pure = true)
    protected final @NotNull EventCallback makeEventCallbackRaw(
            @NotNull EventHolder<?> eventHolder,@Nullable Object holder,@NotNull MethodHandle eventCallback,
            boolean ignoreCancelled, int priority,@Nullable BooleanSupplier validator) {
        this.ensureInstanceAccess();
        return new EventCallback(eventHolder, holder, eventCallback,
                ignoreCancelled, priority, validator);
    }

    /**
     * Used to get the EventCallbacks of an event handler.
     * @param instance the instance to get the event callbacks of
     * @return an {@link ArrayList} of all registered event callbacks
     * @throws EventRegistrationException if we failed to acquire all event callbacks
     * @since 1.0.0
     */
    @Contract(pure = true)
    protected final ArrayList<EventCallback> getEventCallbacks(
            @NotNull Object instance) throws EventRegistrationException {
        return this.getEventCallbacks(instance, null, false, null);
    }

    /**
     * Used to get the EventCallbacks of an event handler.
     * @param instance the instance to get the event callbacks of
     * @param validator the validator, or null if the result is always {@code true}.
     * @return an {@link ArrayList} of all registered event callbacks
     * @throws EventRegistrationException if we failed to acquire all event callbacks
     * @since 1.0.0
     */
    @Contract(pure = true)
    protected final @NotNull ArrayList<EventCallback> getEventCallbacks(
            @NotNull Object instance,@Nullable BooleanSupplier validator) throws EventRegistrationException {
        return this.getEventCallbacks(instance, validator, false, null);
    }

    /**
     * Used to get the EventCallbacks of an event handler.
     * @param instance the instance to get the event callbacks of
     * @param validator the validator, or null if the result is always {@code true}.
     * @param ignoreInvalid should it skip invalid event callback instead of throwing {@link EventRegistrationException}.
     * @return an {@link ArrayList} of all registered event callbacks
     * @throws EventRegistrationException if we failed to acquire all event callbacks
     * @since 1.2.0
     */
    @Contract(pure = true)
    protected final @NotNull ArrayList<EventCallback> getEventCallbacks(
            @NotNull Object instance,@Nullable BooleanSupplier validator,
            boolean ignoreInvalid) throws EventRegistrationException {
        return this.getEventCallbacks(instance, validator, ignoreInvalid, null);
    }

    /**
     * Used to get the EventCallbacks of an event handler.
     * @param instance the instance to get the event callbacks of
     * @param validator the validator, or null if the result is always {@code true}.
     * @param ignoreInvalid should it skip invalid event callback instead of throwing {@link EventRegistrationException}.
     * @param lookup the lookup used to resolve methods
     * @return an {@link ArrayList} of all registered event callbacks
     * @throws EventRegistrationException if we failed to acquire all event callbacks
     * @since 1.2.0
     */
    @Contract(pure = true)
    protected final @NotNull ArrayList<EventCallback> getEventCallbacks(
            @NotNull Object instance, @Nullable BooleanSupplier validator,
            boolean ignoreInvalid, @Nullable MethodHandles.Lookup lookup) throws EventRegistrationException {
        this.ensureInstanceAccess();
        ArrayList<EventCallback> eventCallbacks = new ArrayList<>();
        boolean isStatic = instance instanceof Class;
        Class<?> handlerClass = isStatic ? (Class<?>) instance : instance.getClass();
        if (lookup == null) {
            lookup = MethodHandles.publicLookup();
        }
        for (Method method : handlerClass.getMethods()) {
            if (method.isBridge() || method.isSynthetic() ||
                    Modifier.isStatic(method.getModifiers()) != isStatic) continue;
            EventHandler eventHandler = method.getAnnotation(EventHandler.class);
            if (eventHandler == null) continue;
            Class<?>[] args = method.getParameterTypes();
            if (args.length != 1) continue;
            EventHolder<?> eventHolder = EventHolder.getHolderFromEventRaw(args[0]);
            MethodHandle methodHandle;
            try {
                methodHandle = lookup.unreflect(method);
                if (!isStatic) {
                    methodHandle = methodHandle.bindTo(instance);
                }
            } catch (IllegalAccessException e) {
                if (ignoreInvalid) continue;
                throw new EventRegistrationException("Failed to un-reflect method " + method.getName(), e);
            }
            eventCallbacks.add(new EventCallback(eventHolder, isStatic ? null : instance, methodHandle,
                    eventHandler.ignoreCancelled(), eventHandler.priority(), validator));
        }
        return eventCallbacks;
    }

    /**
     * Allow to add event callback to various event holders
     *
     * @param eventCallback event callback to register
     * @return if the event callback was added
     * @since 1.0.0
     */
    protected final boolean registerEventCallback(@NotNull EventCallback eventCallback) {
        this.ensureInstanceAccess();
        return eventCallback.eventHolder.registerEventCallback(eventCallback);
    }

    /**
     * Allow to add event callback to various event holders
     *
     * @param eventCallback event callback to register
     * @return if the event callback was added
     * @since 1.1.0
     */
    protected final boolean unregisterEventCallback(@NotNull EventCallback eventCallback) {
        this.ensureInstanceAccess();
        return eventCallback.eventHolder.unregisterEventCallback(eventCallback);
    }

    /**
     * Unregister event handlers that are owned by the instance
     *
     * @param classLoader to unregister events from.
     * @param instance to unregister events from.
     * @return if any event has been unregistered
     * @since 1.1.0
     */
    protected final boolean unregisterEventsForClassLoader(@Nullable ClassLoader classLoader, @NotNull Object instance) {
        this.ensureInstanceAccess();
        final boolean[] ret = new boolean[]{false};
        EventHolder.forEachEventHolder(classLoader,
                holder -> ret[0] |= holder.unregisterEventCallbackFromInstance(instance));
        return ret[0];
    }

    /**
     * Method used to register event handles from an instance
     *
     * @param handler event handler to register
     * @throws EventRegistrationException if failed to register the handler.
     * @since 1.0.0
     */
    public abstract void registerEvents(@NotNull Object handler) throws EventRegistrationException;

    /**
     * Method used to register event handles from an instance
     *
     * @param handler event handler to register
     * @param validator used to detect when handlers should be invalid
     * @throws EventRegistrationException if failed to register the handler
     * @since 1.0.0
     */
    public abstract void registerEvents(@NotNull Object handler,@Nullable BooleanSupplier validator) throws EventRegistrationException;

    /**
     * Method used to unregister event handles from an instance
     *
     * @param handler event handler to unregister
     * @since 1.1.0
     */
    public abstract void unregisterEvents(@NotNull Object handler);

    /**
     * Callback called when an event fails to dispatch
     *
     * @param event the event
     * @param eventCallback callback that caused the error
     * @param throwable error
     * @since 1.0.0
     */
    protected void onEventError(@NotNull Event event,@NotNull EventCallback eventCallback,@NotNull Throwable throwable) {
        sneakyThrow(throwable);
    }

    /**
     * Callback called when unsafe APIs are called, can be used to limit the usage of the {@link Unsafe} API
     *
     * @param method name of used unsafe method
     * @throws SecurityException if operation is not permitted
     * @since 1.0.0
     */
    protected void onUnsafeAccess(String method) throws SecurityException {}

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }

    /**
     * @param o1 the first event callback to be compared.
     * @param o2 the second event callback to be compared.
     * @return a negative integer, zero, or a positive integer as
     *   the first event callback should be run after, in any order, or before the second event callback.
     * @since 1.0.0
     */
    public int compare(@NotNull EventCallback o1,@NotNull EventCallback o2) {
        return Integer.compare(o2.priority, o1.priority);
    }

    /**
     * Can be implemented by subclass loader of the classloader that implemented FoxEvent to avoid memory leaks.
     * <p>
     * The class loader that defined FoxEvent doesn't need to implement this class itself.
     *
     * @since 1.0.0
     */
    public interface FoxEventsClassLoader {
        /**
         * Used to store the backing IdentityHashMap in a sub-classloader
         *
         * @return the non-null IdentityHashMap used to store EvenHolders
         * @since 1.0.0
         */
        @Contract(pure = true)
        IdentityHashMap<Class<? extends Event>, EventHolder<?>> getFoxEventHolderReferences();
    }

    /**
     * FoxEvents Unsafe, used to apply unsafe operations, and expose internals of FoxEvents.
     * <p>
     * For advanced users only, misuse of this class may break FoxEvents.
     *
     * @since 1.0.0
     */
    public static final class Unsafe {
        private Unsafe() {}

        /**
         * This is used to give instance access to the FoxEvents implementation of your choice,
         * allowing it to execute privileged event actions even if it isn't the active FoxEvents
         * implementation.
         *
         * @param foxEvents to give instance access to.
         * @since 1.0.0
         */
        public static void giveInstanceAccessUnsafe(@NotNull FoxEvents foxEvents) {
            getFoxEventsSoft().onUnsafeAccess("giveInstanceAccessUnsafe");
            foxEvents.unsafeAccess = true;
        }

        /**
         * Used to force set a FoxEvents implementation as the main instance, even if the main one is already set.
         *
         * @param foxEvents to set as the default implementation.
         * @since 1.0.0
         */
        public static void setFoxEventsUnsafe(@Nullable FoxEvents foxEvents) {
            getFoxEvents().onUnsafeAccess("setFoxEventsUnsafe");
            FoxEvents.foxEvents = foxEvents;
        }

        /**
         * Used to force set cancelled valued on event.
         *
         * @param event     the event
         * @param cancelled if the event should be cancelled
         * @since 1.0.0
         */
        public static void setCancelledUnsafe(@NotNull Event event, boolean cancelled) {
            getFoxEventsSoft().onUnsafeAccess("setCancelledUnsafe");
            event.cancelled = cancelled;
        }

        /**
         * Get the backing event holder hash map of a class loader.
         *
         * @param classLoader to get identity hash map of
         * @return the backing identity hash map
         * @since 1.0.0
         */
        @Contract(value = "null -> null", pure = true)
        public static @Nullable IdentityHashMap<@NotNull Class<? extends Event>,@NotNull EventHolder<?>>
        /*    */ getEventHolderMapUnsafe(@Nullable ClassLoader classLoader) {
            getFoxEvents().onUnsafeAccess("getEventHolderMapUnsafe");
            if (classLoader instanceof FoxEventsClassLoader) {
                return ((FoxEventsClassLoader) classLoader).getFoxEventHolderReferences();
            }
            return EventHolder.eventHoldersCache.get(classLoader);
        }

        /**
         * Unregister event handlers that are owned by the instance
         *
         * @param classLoader to unregister events from.
         * @param instance to unregister events from.
         * @return if any event has been unregistered
         * @since 1.1.0
         */
        public static boolean unregisterEventsForClassLoaderUnsafe(
                @Nullable ClassLoader classLoader, @NotNull Object instance) {
            getFoxEvents().onUnsafeAccess("unregisterEventsUnsafe");
            final boolean[] ret = new boolean[]{false};
            EventHolder.forEachEventHolder(classLoader,
                    holder -> ret[0] |= holder.unregisterEventCallbackFromInstance(instance));
            return ret[0];
        }
    }
}
