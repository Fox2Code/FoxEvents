package com.fox2code.foxevents;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @param <T> event type
 * @since 1.0.0
 */
public final class EventHolder<T extends Event> {
    private static final Predicate<EventCallback> pInvalid = EventCallback::isInvalid;
    private static final Predicate<EventCallback> pIgnoreCancelled = p -> p.ignoreCancelled;
    private static final EventCallback[] EMPTY_EVENT_CALLBACKS = new EventCallback[0];
    static final WeakHashMap<ClassLoader, IdentityHashMap<Class<? extends Event>, EventHolder<?>>>
            eventHoldersCache = new WeakHashMap<>();
    private static final Function<ClassLoader, IdentityHashMap<Class<? extends Event>, EventHolder<?>>>
            eventHoldersClassLoaderMapProvider = classLoader -> new IdentityHashMap<>();
    private static final Function<Class<? extends Event>, EventHolder<?>>
            eventHolderProvider = EventHolder::new;
    private static final boolean ignoreMemoryLeaks = Boolean.getBoolean("foxevents.ignore-memory-leaks");
    private static final HashSet<String> printedClassLoaderMemoryLeakIssues = new HashSet<>();
    private static volatile boolean fullWarn = true;
    static int validationModCount = 0;

    /**
     * get the holder of an event class, it is recommended to cache holders statically,
     * as {@code getHolderFromEvent()} is usually a bit slow, especially with millions
     * of events per second.
     *
     * @param event class to get EventHolder of
     * @param <T> inferred type of the event
     * @return the event EventHolder
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public static @NotNull <T extends Event> EventHolder<T> getHolderFromEvent(@NotNull Class<T> event) {
        return (EventHolder<T>) getHolderFromEventRaw(event);
    }

    /**
     * Same as {@link #getHolderFromEvent(Class)}, but without generic type in the signature.
     *
     * @param event class to get EventHolder of
     * @return the event EventHolder
     * @since 1.0.0
     */
    public static @NotNull EventHolder<?> getHolderFromEventRaw(@NotNull Class<?> event) {
        Class<? extends Event> eventClass = event.asSubclass(Event.class);
        ClassLoader classLoader = eventClass.getClassLoader();
        assert classLoader != null;
        if (classLoader instanceof FoxEvents.FoxEventsClassLoader) {
            FoxEvents.FoxEventsClassLoader foxEventsClassLoader =
                    (FoxEvents.FoxEventsClassLoader) classLoader;
            return foxEventsClassLoader.getFoxEventHolderReferences()
                    .computeIfAbsent(eventClass, eventHolderProvider);
        }
        // Warn about memory leak we can't solve properly alone.
        String classLoaderName = classLoader.getClass().getName();
        if (classLoader != EventHolder.class.getClassLoader() &&
                classLoader != ClassLoader.getSystemClassLoader() &&
                classLoader != FoxEvents.getFoxEventsSoft().getClass().getClassLoader() &&
                printedClassLoaderMemoryLeakIssues.add(classLoaderName) && !ignoreMemoryLeaks) {
            FoxEvents.LOGGER.warning("Possible memory-leak while registering " + event.getName() +
                    " on class loader " + classLoaderName);
            if (fullWarn) {
                fullWarn = false;
                FoxEvents.LOGGER.warning("To fix the issue, implement " +
                        "the FoxEventClassLoader interface on " + classLoaderName);
                FoxEvents.LOGGER.warning("If you do not unload that ClassLoaders " +
                        "during your app lifecycle, you can safely ignore this message");
            }
        }
        return eventHoldersCache.computeIfAbsent(classLoader, eventHoldersClassLoaderMapProvider)
                .computeIfAbsent(eventClass, eventHolderProvider);
    }

    private final ArrayList<EventCallback> eventCallbacks = new ArrayList<>();
    private EventCallback[] bakedCallbacks = EMPTY_EVENT_CALLBACKS;
    private boolean bakedCallbacksSkipOnCancelled = false;
    private WeakHashMap<EventHolder<?>, Void> delegatedChilds;
    private final EventHolder<? super T> delegate;
    private final WeakReference<Class<T>> event;
    private final String eventName;
    private final int eventModifiers;
    private final boolean cancellable;
    private int validationCount;

    @SuppressWarnings("unchecked")
    private EventHolder(Class<T> event) {
        this.event = new WeakReference<>(event);
        this.eventName = event.getName();
        this.eventModifiers = event.getModifiers();
        boolean cancellable = Event.Cancellable.class.isAssignableFrom(event);
        boolean delegate = event.getDeclaredAnnotation(Event.DelegateEvent.class) != null;
        this.delegate = delegate ? (EventHolder<? super T>) getHolderFromEventRaw(event.getSuperclass()) : null;
        if (this.delegate != null) this.delegate.getDelegatedChilds().put(this, null);
        this.cancellable = cancellable;
    }

    /**
     * @return the event class
     * @throws IllegalStateException if the event has been freed
     * @since 1.0.0
     */
    @NotNull
    public Class<T> getEvent() throws IllegalStateException {
        Class<T> eventClass = this.event.get();
        if (eventClass == null) {
            throw new IllegalStateException("Event class has been freed by the JVM");
        }
        return eventClass;
    }

    /**
     * @return the event class
     * @since 1.0.0
     */
    @Nullable
    public Class<T> peekEvent() {
        return this.event.get();
    }

    /**
     * @return the event name
     * @since 1.0.0
     */
    @NotNull
    public String getEventName() {
        return eventName;
    }

    /**
     * @return if no listeners are registered
     * @since 1.0.0
     */
    @Contract(pure = true)
    public boolean isEmpty() {
        return this.eventCallbacks.isEmpty() &&
                (this.delegate == null || this.delegate.isEmpty());
    }

    /**
     * @return if event is abstract
     * @since 1.0.0
     */
    @Contract(pure = true)
    public boolean isAbstract() {
        return Modifier.isAbstract(this.eventModifiers);
    }

    /**
     * @return if event will delegate handler to it's parent
     * @since 1.0.0
     */
    @Contract(pure = true)
    public boolean isDelegate() {
        return this.delegate != null;
    }

    /**
     * @return if event is cancellable
     * @since 1.0.0
     */
    @Contract(pure = true)
    public boolean isCancellable() {
        return this.cancellable;
    }

    /**
     * Call event through the event pipeline
     * @param event to dispatch
     * @since 1.0.0
     */
    public void callEvent(@NotNull T event) {
        Objects.requireNonNull(event, "event == null");
        this.getEvent().cast(event);
        this.callEventRaw(event);
    }

    /**
     * Allow to ensure that event handlers are baked.
     * @since 1.0.0
     */
    public void ensureBaked() {
        if (this.bakedCallbacks == null ||
                this.validationCount != validationModCount) {
            this.callEventRaw(null);
        }
    }

    void callEventRaw(Event event) {
        EventCallback[] bakedCallbacks;
        synchronized (this.eventCallbacks) {
            bakedCallbacks = this.bakedCallbacks;
            if (bakedCallbacks == null ||
                    this.validationCount != validationModCount) {
                this.validationCount = validationModCount;
                ArrayList<EventCallback> eventCallbacks;
                if (this.delegate == null) {
                    this.eventCallbacks.removeIf(pInvalid);
                    eventCallbacks = this.eventCallbacks;
                } else {
                    eventCallbacks = new ArrayList<>();
                    this.collectHandlers(eventCallbacks);
                }
                eventCallbacks.sort(FoxEvents.getFoxEventsSoft().comparator);
                this.bakedCallbacks = bakedCallbacks =
                        eventCallbacks.toArray(EMPTY_EVENT_CALLBACKS);
                this.bakedCallbacksSkipOnCancelled =
                        eventCallbacks.stream().noneMatch(pIgnoreCancelled);
            }
        }
        if (event == null) return;
        if (this.bakedCallbacksSkipOnCancelled) {
            for (EventCallback eventCallback : bakedCallbacks) {
                if (event.cancelled) return;
                eventCallback.callForEvent(event);
            }
        } else {
            for (EventCallback eventCallback : bakedCallbacks) {
                if (event.cancelled && !eventCallback.ignoreCancelled) continue;
                eventCallback.callForEvent(event);
            }
        }
    }

    boolean registerEventCallback(EventCallback eventCallback) {
        if (eventCallback == null || (
                eventCallback.validator != null &&
                        !eventCallback.validator.getAsBoolean())) {
            // Check validator before adding
            return false;
        }
        if (eventCallback.eventHolder != this) {
            // We should never reach this code, but just in case.
            throw new IllegalArgumentException("EventCallback.eventHolder != this");
        }
        return this.registerEventCallbackRaw(eventCallback);
    }

    private boolean registerEventCallbackRaw(EventCallback eventCallback) {
        boolean added = false;
        synchronized (this.eventCallbacks) {
            if (!this.eventCallbacks.contains(eventCallback)) {
                added = this.eventCallbacks.add(eventCallback);
                this.bakedCallbacks = null;
            }
        }
        if (this.delegatedChilds != null && added) {
            this.markChildsDirty();
        }
        return added;
    }

    private WeakHashMap<EventHolder<?>, Void> getDelegatedChilds() {
        if (this.delegatedChilds == null) {
            this.delegatedChilds = new WeakHashMap<>();
        }
        return this.delegatedChilds;
    }

    private void markChildsDirty() {
        WeakHashMap<EventHolder<?>, Void> delegatedChilds = this.delegatedChilds;
        if (delegatedChilds != null) {
            for (EventHolder<?> eventHolder : delegatedChilds.keySet()) {
                synchronized (eventHolder.eventCallbacks) {
                    eventHolder.bakedCallbacks = null;
                }
                eventHolder.markChildsDirty();
            }
        }
    }

    private void collectHandlers(ArrayList<EventCallback> eventCallbacks) {
        this.eventCallbacks.removeIf(pInvalid);
        eventCallbacks.addAll(this.eventCallbacks);
        if (this.delegate != null) {
            this.delegate.collectHandlers(eventCallbacks);
        }
    }
}
